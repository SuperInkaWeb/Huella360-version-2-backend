package com.vet_saas.modules.delivery.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.delivery.dto.request.CalificacionDTO;
import com.vet_saas.modules.delivery.dto.request.ConfirmarOTPDTO;
import com.vet_saas.modules.delivery.dto.request.CrearDeliveryDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryEmpresaResponseDTO;
import com.vet_saas.modules.delivery.dto.response.EstadoDeliveryEvent;
import com.vet_saas.modules.delivery.event.DeliveryStatusChangedEvent;
import com.vet_saas.modules.delivery.mapper.DeliveryMapper;
import com.vet_saas.modules.delivery.model.*;
import com.vet_saas.modules.delivery.repository.DeliveryEstadoRepository;
import com.vet_saas.modules.delivery.repository.DeliveryRepository;
import com.vet_saas.modules.delivery.repository.RepartidorRepository;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.sales.model.EstadoOrden;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEstadoRepository estadoRepository;
    private final RepartidorRepository repartidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final StorageService cloudinaryService;
    private final SimpMessagingTemplate wsTemplate;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryMapper deliveryMapper;
    private final EmpresaRepository empresaRepository;
    private final EmailService emailService;
    private final PointsService pointsService;
    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    private static final int OTP_EXPIRACION_HORAS = 4;
    private static final String CLOUDINARY_FOLDER_DELIVERY = "deliveries/confirmaciones";

    private static final List<DeliveryStatus> ESTADOS_FINALES = List.of(
        DeliveryStatus.ENTREGADO, DeliveryStatus.FALLIDO,
        DeliveryStatus.CANCELADO, DeliveryStatus.INCIDENCIA
    );

    // =========================================================
    // CREAR DELIVERY
    // =========================================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryResponseDTO crearDelivery(Orden orden, CrearDeliveryDTO dto) {
        if (deliveryRepository.existsByOrdenId(orden.getId())) {
            log.warn("Delivery ya existe para la orden {}", orden.getId());
            return null;
        }

        if (dto.getOrigenLat() == null || dto.getOrigenLng() == null) {
            throw new BusinessException(
                "No se puede crear el delivery: la veterinaria no tiene coordenadas configuradas"
            );
        }
        if (dto.getDestinoLat() == null || dto.getDestinoLng() == null) {
            throw new BusinessException(
                "No se puede crear el delivery: la orden no tiene coordenadas de destino"
            );
        }

        double distanciaKm = calcularDistanciaHaversine(
            dto.getOrigenLat().doubleValue(), dto.getOrigenLng().doubleValue(),
            dto.getDestinoLat().doubleValue(), dto.getDestinoLng().doubleValue()
        );
        int tiempoEstimadoMin = Math.max(10, (int) Math.ceil(distanciaKm / 0.5));

        Delivery delivery = Delivery.builder()
            .orden(orden)
            .origenLat(dto.getOrigenLat())
            .origenLng(dto.getOrigenLng())
            .origenDireccion(dto.getOrigenDireccion())
            .destinoLat(dto.getDestinoLat())
            .destinoLng(dto.getDestinoLng())
            .destinoDireccion(dto.getDestinoDireccion())
            .destinoReferencia(dto.getDestinoReferencia())
            .costoDelivery(dto.getCostoDelivery())
            .distanciaKm(new java.math.BigDecimal(String.format("%.2f", distanciaKm)))
            .tiempoEstimadoMin(tiempoEstimadoMin)
            .estado(DeliveryStatus.BUSCANDO_REPARTIDOR)
            .intentosAsignacion(0)
            .build();

        delivery = deliveryRepository.saveAndFlush(delivery);
        registrarEstado(delivery, DeliveryStatus.BUSCANDO_REPARTIDOR, "Delivery creado por pago de orden", null);

        String otpPlano = generarYGuardarOTP(delivery);
        emailService.sendDeliveryOtpEmail(orden.getId(), otpPlano);

        wsTemplate.convertAndSend("/topic/pedidos-disponibles", deliveryMapper.toResponseDTO(delivery));

        return deliveryMapper.toResponseDTOConOTP(delivery, otpPlano);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getPedidosDisponibles() {
        return deliveryRepository.findByEstado(DeliveryStatus.BUSCANDO_REPARTIDOR)
            .stream()
            .map(deliveryMapper::toResponseDTO)
            .collect(Collectors.toList());
    }

    // =========================================================
    // PUNTO 1: ACEPTAR PEDIDO - bloqueos sobre delivery y repartidor
    // =========================================================
    public DeliveryResponseDTO aceptarPedido(Long deliveryId, Long usuarioId) {
        // Esta lectura genera SELECT ... FOR UPDATE. La validacion del estado y la
        // asignacion quedan serializadas para un mismo delivery hasta el commit.
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado"));

        if (delivery.getEstado() != DeliveryStatus.BUSCANDO_REPARTIDOR) {
            throw new BusinessException("El pedido ya no está disponible para asignación");
        }

        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("No eres un repartidor registrado"));

        Repartidor repartidorLockeado = repartidorRepository.findByIdForUpdate(repartidor.getIdRepartidor())
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado para bloqueo"));

        int activos = deliveryRepository.countDeliveriesActivos(
            repartidorLockeado.getIdRepartidor(), ESTADOS_FINALES
        );
        if (activos >= repartidorLockeado.getMaxPedidosSimultaneos()) {
            throw new BusinessException(
                "Has alcanzado el límite de " + repartidorLockeado.getMaxPedidosSimultaneos() +
                " pedidos simultáneos. Termínalo antes de tomar otro.");
        }

        delivery.setRepartidor(repartidorLockeado);
        delivery.setEstado(DeliveryStatus.REPARTIDOR_ASIGNADO);
        delivery.setAsignadoAt(Instant.now());
        deliveryRepository.save(delivery);

        int nuevosActivos = activos + 1;
        if (nuevosActivos >= repartidorLockeado.getMaxPedidosSimultaneos()) {
            repartidorLockeado.setEstadoActual(RepartidorStatus.OCUPADO);
            repartidorRepository.save(repartidorLockeado);
        }

        registrarEstado(delivery, DeliveryStatus.REPARTIDOR_ASIGNADO, "Pedido aceptado por el repartidor", usuarioId);

        wsTemplate.convertAndSend("/topic/delivery/" + deliveryId + "/estado",
            EstadoDeliveryEvent.builder()
                .deliveryId(deliveryId)
                .estado(DeliveryStatus.REPARTIDOR_ASIGNADO)
                .descripcion("Pedido aceptado por el repartidor")
                .timestamp(Instant.now())
                .build()
        );
        wsTemplate.convertAndSend("/topic/pedidos-pool-update", deliveryId);

        return deliveryMapper.toResponseDTO(delivery);
    }

    // =========================================================
    // CONSULTAR DELIVERY
    // =========================================================
    @Transactional(readOnly = true)
    public DeliveryResponseDTO getByOrdenId(Long ordenId) {
        Delivery delivery = deliveryRepository.findByOrdenId(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado para orden: " + ordenId));
        return deliveryMapper.toResponseDTO(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponseDTO getById(Long deliveryId) {
        return deliveryMapper.toResponseDTO(
            deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId))
        );
    }

    // =========================================================
    // PUNTO 2: RECHAZAR PEDIDO - UPDATE atómico
    // =========================================================
    public DeliveryResponseDTO rechazarPedido(Long deliveryId, Long usuarioId) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado"));
        Long repartidorId = repartidor.getIdRepartidor();
        int filasAfectadas = deliveryRepository.liberarSiAsignadoA(deliveryId, repartidorId);

        if (filasAfectadas == 0) {
            Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

            if (delivery.getRepartidor() == null || !delivery.getRepartidor().getIdRepartidor().equals(repartidorId)) {
                throw new BusinessException("No eres el repartidor asignado a este delivery");
            }
            if (delivery.getEstado() == DeliveryStatus.RECOGIDO ||
                delivery.getEstado() == DeliveryStatus.EN_CAMINO ||
                delivery.getEstado() == DeliveryStatus.CERCA) {
                throw new BusinessException("No puedes cancelar un pedido que ya fue recogido");
            }
            throw new BusinessException("El pedido ya fue liberado por otra operación");
        }

        repartidorRepository.actualizarEstado(repartidorId, RepartidorStatus.DISPONIBLE);

        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));
        wsTemplate.convertAndSend("/topic/pedidos-disponibles", deliveryMapper.toResponseDTO(delivery));

        wsTemplate.convertAndSend("/topic/delivery/" + deliveryId + "/estado",
            EstadoDeliveryEvent.builder()
                .deliveryId(deliveryId)
                .estado(DeliveryStatus.BUSCANDO_REPARTIDOR)
                .descripcion("El repartidor canceló el pedido. Buscando nuevo repartidor.")
                .timestamp(Instant.now())
                .build()
        );

        return deliveryMapper.toResponseDTO(delivery);
    }

    // =========================================================
    // CAMBIAR ESTADO
    // =========================================================
    public DeliveryResponseDTO cambiarEstado(Long deliveryId, DeliveryStatus nuevoEstado,
                                              Long usuarioId, String descripcion) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (delivery.getEstado().esFinal()) {
            throw new BusinessException("El delivery ya está en estado final: " + delivery.getEstado());
        }

        if (!delivery.getEstado().puedeTransicionarA(nuevoEstado)) {
            throw new BusinessException(
                String.format("Transición inválida: %s → %s", delivery.getEstado(), nuevoEstado)
            );
        }

        DeliveryStatus estadoAnterior = delivery.getEstado();
        delivery.setEstado(nuevoEstado);
        marcarTimestamp(delivery, nuevoEstado);

        if (nuevoEstado.esFinal() && delivery.getRepartidor() != null) {
            repartidorRepository.actualizarEstado(
                delivery.getRepartidor().getIdRepartidor(),
                RepartidorStatus.DISPONIBLE
            );
            if (nuevoEstado == DeliveryStatus.ENTREGADO) {
                delivery.getRepartidor().setTotalEntregas(
                    delivery.getRepartidor().getTotalEntregas() + 1
                );
                repartidorRepository.save(delivery.getRepartidor());
            }
        }

        delivery = deliveryRepository.save(delivery);
        registrarEstado(delivery, nuevoEstado, descripcion, usuarioId);

        wsTemplate.convertAndSend(
            "/topic/delivery/" + deliveryId + "/estado",
            EstadoDeliveryEvent.builder()
                .deliveryId(deliveryId)
                .estado(nuevoEstado)
                .descripcion(descripcion)
                .timestamp(Instant.now())
                .build()
        );

        publishStatusChanged(delivery, estadoAnterior, nuevoEstado, descripcion);

        return deliveryMapper.toResponseDTO(delivery);
    }

    @Transactional
    public DeliveryResponseDTO cancelarDelivery(Long deliveryId, Long usuarioId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (!delivery.getOrden().getUsuarioCliente().getId().equals(usuarioId)) {
            throw new BusinessException("No autorizado para cancelar este delivery");
        }

        if (delivery.getEstado() == DeliveryStatus.RECOGIDO ||
            delivery.getEstado() == DeliveryStatus.EN_CAMINO ||
            delivery.getEstado() == DeliveryStatus.CERCA) {
            throw new BusinessException("No se puede cancelar el envío porque el pedido ya está en manos del repartidor.");
        }

        if (delivery.getEstado().esFinal()) {
            throw new BusinessException("El delivery ya ha finalizado.");
        }

        DeliveryStatus estadoAnterior = delivery.getEstado();
        delivery.setEstado(DeliveryStatus.CANCELADO);
        deliveryRepository.save(delivery);
        registrarEstado(delivery, DeliveryStatus.CANCELADO, "Venta cancelada por el cliente", usuarioId);

        if (delivery.getRepartidor() != null) {
            repartidorRepository.actualizarEstado(
                delivery.getRepartidor().getIdRepartidor(),
                RepartidorStatus.DISPONIBLE
            );
        }

        wsTemplate.convertAndSend("/topic/delivery/" + deliveryId + "/estado",
            EstadoDeliveryEvent.builder()
                .deliveryId(deliveryId)
                .estado(DeliveryStatus.CANCELADO)
                .descripcion("Venta cancelada por el cliente")
                .timestamp(Instant.now())
                .build()
        );

        publishStatusChanged(delivery, estadoAnterior, DeliveryStatus.CANCELADO, "Venta cancelada por el cliente");

        return deliveryMapper.toResponseDTO(delivery);
    }

    // =========================================================
    // CONFIRMACION POR OTP
    // =========================================================
    public void confirmarEntregaOTP(Long deliveryId, ConfirmarOTPDTO dto, Long usuarioId) {
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado"));
        if (delivery.getRepartidor() == null
                || !delivery.getRepartidor().getIdRepartidor().equals(repartidor.getIdRepartidor())) {
            throw new BusinessException("No autorizado para confirmar este delivery");
        }

        if (delivery.getEstado() != DeliveryStatus.EN_CAMINO
                && delivery.getEstado() != DeliveryStatus.CERCA) {
            throw new BusinessException("Solo se puede confirmar cuando el repartidor está en camino o cerca");
        }

        if (delivery.getCodigoExpiraAt() == null || delivery.getCodigoExpiraAt().isBefore(Instant.now())) {
            throw new BusinessException("El código OTP ha expirado");
        }

        if (!passwordEncoder.matches(dto.getCodigo(), delivery.getCodigoConfirmacion())) {
            throw new BusinessException("Código OTP incorrecto");
        }

        if (delivery.getFotoEntregaUrl() == null || delivery.getFotoEntregaUrl().isBlank()) {
            throw new BusinessException("Debes subir una foto de evidencia antes de confirmar la entrega");
        }

        cambiarEstado(deliveryId, DeliveryStatus.ENTREGADO, usuarioId, "Confirmado con PIN y foto de evidencia");
    }

    /** Regenera el PIN del propietario de la compra e invalida inmediatamente el anterior. */
    public String regenerarOTP(Long deliveryId, Long usuarioId) {
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (delivery.getOrden().getUsuarioCliente() == null
                || !delivery.getOrden().getUsuarioCliente().getId().equals(usuarioId)) {
            throw new BusinessException("No autorizado para actualizar el PIN de este delivery");
        }
        if (delivery.getEstado().esFinal()) {
            throw new BusinessException("No se puede actualizar el PIN de un delivery finalizado");
        }
        if (delivery.getRepartidor() == null) {
            throw new BusinessException("El PIN solo puede generarse cuando ya existe un repartidor asignado");
        }

        String otpPlano = generarYGuardarOTP(delivery);
        return otpPlano;
    }

    // =========================================================
    // CONFIRMACION POR FOTO
    // =========================================================
    public void confirmarEntregaFoto(Long deliveryId, MultipartFile foto, Long usuarioId) {
        if (foto == null || foto.isEmpty()) {
            throw new BusinessException("La foto de evidencia es obligatoria");
        }
        if (foto.getContentType() == null || !foto.getContentType().startsWith("image/")) {
            throw new BusinessException("El archivo de evidencia debe ser una imagen");
        }

        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado"));

        if (delivery.getRepartidor() == null ||
                !delivery.getRepartidor().getIdRepartidor().equals(repartidor.getIdRepartidor())) {
            throw new BusinessException("No autorizado para confirmar este delivery");
        }
        if (delivery.getEstado() != DeliveryStatus.EN_CAMINO
                && delivery.getEstado() != DeliveryStatus.CERCA) {
            throw new BusinessException("La evidencia solo puede subirse cuando el pedido está en camino o cerca");
        }

        String fotoUrl = cloudinaryService.uploadFile(foto, CLOUDINARY_FOLDER_DELIVERY);
        delivery.setFotoEntregaUrl(fotoUrl);
        deliveryRepository.save(delivery);
    }

    // =========================================================
    // INTENTO FALLIDO
    // =========================================================
    public void reportarIntentoFallido(Long deliveryId, MultipartFile foto, String motivo, Long repartidorId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (foto != null && !foto.isEmpty()) {
            String fotoUrl = cloudinaryService.uploadFile(foto, CLOUDINARY_FOLDER_DELIVERY + "/fallidos");
            delivery.setFotoEntregaUrl(fotoUrl);
        }

        deliveryRepository.save(delivery);
        cambiarEstado(deliveryId, DeliveryStatus.FALLIDO, repartidorId,
            "Intento fallido: " + (motivo != null ? motivo : "Nadie recibió el pedido"));
    }

    // =========================================================
    // REPORTE DE INCIDENCIA
    // =========================================================
    @Transactional
    public void reportarIncidencia(Long deliveryId, String motivo, String descripcion, MultipartFile foto, Long usuarioId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado para usuario: " + usuarioId));
        Long repartidorId = repartidor.getIdRepartidor();

        if (delivery.getRepartidor() == null ||
                !delivery.getRepartidor().getIdRepartidor().equals(repartidorId)) {
            throw new BusinessException("No autorizado para reportar incidencia en este delivery");
        }

        if (delivery.getEstado().esFinal()) {
            throw new BusinessException("No se puede reportar incidencia en un delivery finalizado");
        }

        if (foto != null && !foto.isEmpty()) {
            String fotoUrl = cloudinaryService.uploadFile(foto, CLOUDINARY_FOLDER_DELIVERY + "/incidencias");
            delivery.setFotoEntregaUrl(fotoUrl);
        }

        String logMsg = "Incidencia reportada: " + motivo + ". " + descripcion;
        DeliveryStatus estadoAnterior = delivery.getEstado();

        if (delivery.getEstado().ordinal() < DeliveryStatus.RECOGIDO.ordinal()) {
            log.info("Incidencia antes de recoger para delivery {}. Reiniciando búsqueda.", deliveryId);
            delivery.setEstado(DeliveryStatus.BUSCANDO_REPARTIDOR);
            delivery.setRepartidor(null);
            deliveryRepository.save(delivery);
            registrarEstado(delivery, DeliveryStatus.BUSCANDO_REPARTIDOR, logMsg, repartidorId);
            wsTemplate.convertAndSend("/topic/pedidos-disponibles", deliveryMapper.toResponseDTO(delivery));
            wsTemplate.convertAndSend("/topic/delivery/" + deliveryId + "/estado",
                EstadoDeliveryEvent.builder()
                    .deliveryId(deliveryId)
                    .estado(DeliveryStatus.BUSCANDO_REPARTIDOR)
                    .descripcion(logMsg)
                    .timestamp(Instant.now())
                    .build()
            );
        } else {
            log.info("Incidencia después de recoger para delivery {}. Marcando orden como FALLIDA.", deliveryId);
            delivery.setEstado(DeliveryStatus.INCIDENCIA);
            delivery.getOrden().setEstado(EstadoOrden.FALLIDO);
            deliveryRepository.save(delivery);
            registrarEstado(delivery, DeliveryStatus.INCIDENCIA, logMsg, repartidorId);
            wsTemplate.convertAndSend("/topic/delivery/" + deliveryId + "/estado",
                EstadoDeliveryEvent.builder()
                    .deliveryId(deliveryId)
                    .estado(DeliveryStatus.INCIDENCIA)
                    .descripcion(logMsg)
                    .timestamp(Instant.now())
                    .build()
            );
        }

        repartidorRepository.actualizarEstado(repartidorId, RepartidorStatus.DISPONIBLE);
        publishStatusChanged(delivery, estadoAnterior, delivery.getEstado(), logMsg);
    }

    // =========================================================
    // REINTENTAR DELIVERY
    // =========================================================
    @Transactional
    public DeliveryResponseDTO reintentarDelivery(Long deliveryId, Long usuarioId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));
        return reintentarInternal(delivery, usuarioId);
    }

    @Transactional
    public DeliveryResponseDTO reintentarDeliveryByOrder(Long ordenId, Long usuarioId) {
        Delivery delivery = deliveryRepository.findByOrdenId(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró delivery para la orden: " + ordenId));
        return reintentarInternal(delivery, usuarioId);
    }

    @Transactional
    public DeliveryResponseDTO reintentarDeliveryCliente(Long deliveryId, Long usuarioId) {
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (delivery.getOrden().getUsuarioCliente() == null
                || !delivery.getOrden().getUsuarioCliente().getId().equals(usuarioId)) {
            throw new BusinessException("No autorizado para reintentar este delivery");
        }
        return reintentarInternal(delivery, usuarioId);
    }

    private DeliveryResponseDTO reintentarInternal(Delivery delivery, Long usuarioId) {
        if (delivery.getEstado() != DeliveryStatus.FALLIDO && delivery.getEstado() != DeliveryStatus.INCIDENCIA) {
            throw new BusinessException("Solo se pueden reintentar pedidos fallidos o con incidencia");
        }

        log.info("Reiniciando delivery para orden {} por solicitud del usuario {}",
            delivery.getOrden().getId(), usuarioId);

        delivery.setEstado(DeliveryStatus.BUSCANDO_REPARTIDOR);
        delivery.setRepartidor(null);
        delivery.setFotoEntregaUrl(null);
        delivery.getOrden().setEstado(EstadoOrden.PAGADO);

        deliveryRepository.save(delivery);
        registrarEstado(delivery, DeliveryStatus.BUSCANDO_REPARTIDOR, "Reintento de envío solicitado", usuarioId);

        wsTemplate.convertAndSend("/topic/pedidos-disponibles", deliveryMapper.toResponseDTO(delivery));
        wsTemplate.convertAndSend("/topic/delivery/" + delivery.getId() + "/estado",
            EstadoDeliveryEvent.builder()
                .deliveryId(delivery.getId())
                .estado(DeliveryStatus.BUSCANDO_REPARTIDOR)
                .descripcion("Reintento de envío solicitado")
                .timestamp(Instant.now())
                .build()
        );

        return deliveryMapper.toResponseDTO(delivery);
    }

    // =========================================================
    // CALIFICACION
    // =========================================================
    public void calificarEntrega(Long deliveryId, CalificacionDTO dto, Long clienteId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (delivery.getEstado() != DeliveryStatus.ENTREGADO) {
            throw new BusinessException("Solo se puede calificar un delivery entregado");
        }

        if (!delivery.getOrden().getUsuarioCliente().getId().equals(clienteId)) {
            throw new BusinessException("No autorizado para calificar este delivery");
        }

        if (delivery.getCalificacionRepartidor() != null) {
            throw new BusinessException("Ya calificaste esta entrega");
        }

        delivery.setCalificacionRepartidor(dto.getCalificacionRepartidor());
        delivery.setComentarioCliente(dto.getComentarioRepartidor());
        delivery.setCalificacionProducto(dto.getCalificacionProducto());
        delivery.setComentarioProducto(dto.getComentarioProducto());
        deliveryRepository.save(delivery);

        recalcularCalificacionRepartidor(delivery.getRepartidor().getIdRepartidor());

        try {
            clienteRepository.findByUsuarioId(clienteId).ifPresent(perfil -> {
                 pointsService.addPoints(perfil.getId(), "CALIFICAR_DELIVERY", delivery.getId(), "Gracias por calificar tu entrega");
            });
        } catch (Exception e) {
            log.error("Error adding points for rating delivery: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getRatingsByUsuarioEmpresa(Long usuarioId) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuarioId)
            .orElseThrow(() -> new BusinessException("No se encontró una empresa vinculada a este usuario"));
        return deliveryRepository.findRatingsByEmpresa(empresa.getId())
            .stream()
            .map(deliveryMapper::toResponseDTO)
            .collect(Collectors.toList());
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================
    private void publishStatusChanged(Delivery delivery, DeliveryStatus estadoAnterior,
                                       DeliveryStatus estadoNuevo, String descripcion) {
        String nombreRepartidor = null;
        if (delivery.getRepartidor() != null) {
            nombreRepartidor = delivery.getRepartidor().getNombres() + " " +
                              delivery.getRepartidor().getApellidos();
        }
        String emailCliente = null;
        if (delivery.getOrden() != null && delivery.getOrden().getUsuarioCliente() != null) {
            emailCliente = delivery.getOrden().getUsuarioCliente().getCorreo();
        }
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(
            this, delivery.getId(),
            delivery.getOrden() != null ? delivery.getOrden().getId() : null,
            estadoAnterior, estadoNuevo, descripcion, nombreRepartidor, emailCliente
        ));
    }

    private String generarYGuardarOTP(Delivery delivery) {
        SecureRandom random = new SecureRandom();
        for (int intento = 0; intento < 20; intento++) {
            String codigo = String.valueOf(100000 + random.nextInt(900000));
            String fingerprint = fingerprintOTP(codigo);
            int reservado = jdbcTemplate.update(
                "INSERT INTO delivery_otp_history (otp_fingerprint, delivery_id) VALUES (?, ?) " +
                    "ON CONFLICT (otp_fingerprint) DO NOTHING",
                fingerprint, delivery.getId()
            );
            if (reservado == 1) {
                delivery.setCodigoConfirmacion(passwordEncoder.encode(codigo));
                delivery.setCodigoExpiraAt(Instant.now().plus(OTP_EXPIRACION_HORAS, ChronoUnit.HOURS));
                deliveryRepository.save(delivery);
                return codigo;
            }
            log.debug("PIN ya utilizado; generando otro para delivery {}", delivery.getId());
        }
        throw new BusinessException("No se pudo generar un PIN único. Intenta nuevamente");
    }

    @Transactional(readOnly = true)
    public List<DeliveryEmpresaResponseDTO> getSeguimientoEmpresa(Long usuarioId) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada para el usuario autenticado"));
        return deliveryRepository.findSeguimientoByEmpresaId(empresa.getId()).stream()
            .map(deliveryMapper::toEmpresaResponseDTO)
            .toList();
    }

    /** Confirma que la empresa revisó una entrega finalizada y cierra su seguimiento GPS. */
    @Transactional
    public DeliveryEmpresaResponseDTO confirmarEntregaEmpresa(Long deliveryId, Long usuarioId) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada para el usuario autenticado"));
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery no encontrado: " + deliveryId));

        if (delivery.getOrden() == null || delivery.getOrden().getEmpresa() == null
                || !delivery.getOrden().getEmpresa().getId().equals(empresa.getId())) {
            throw new BusinessException("No autorizado para confirmar este delivery");
        }
        if (delivery.getEstado() != DeliveryStatus.ENTREGADO) {
            throw new BusinessException("Solo se puede confirmar un pedido que ya fue entregado al cliente");
        }

        if (delivery.getEmpresaConfirmadoAt() == null) {
            delivery.setEmpresaConfirmadoAt(Instant.now());
            delivery = deliveryRepository.save(delivery);
        }
        return deliveryMapper.toEmpresaResponseDTO(delivery);
    }

    private String fingerprintOTP(String codigo) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private void marcarTimestamp(Delivery delivery, DeliveryStatus estado) {
        Instant ahora = Instant.now();
        switch (estado) {
            case REPARTIDOR_ASIGNADO -> delivery.setAsignadoAt(ahora);
            case EN_TIENDA           -> delivery.setEnTiendaAt(ahora);
            case RECOGIDO            -> delivery.setRecogidoAt(ahora);
            case ENTREGADO           -> delivery.setEntregadoAt(ahora);
            default -> {}
        }
    }

    private void registrarEstado(Delivery delivery, DeliveryStatus estado,
                                  String descripcion, Long usuarioId) {
        estadoRepository.save(DeliveryEstado.builder()
            .delivery(delivery)
            .estado(estado)
            .descripcion(descripcion)
            .build());
    }

    private void recalcularCalificacionRepartidor(Long repartidorId) {
        Double promedio = deliveryRepository
            .findCalificacionesByRepartidor(repartidorId)
            .stream()
            .mapToInt(d -> d.getCalificacionRepartidor().intValue())
            .average()
            .orElse(5.0);
        repartidorRepository.findById(repartidorId).ifPresent(r -> {
            r.setCalificacionPromedio(new java.math.BigDecimal(String.format("%.2f", promedio)));
            repartidorRepository.save(r);
        });
    }

    private double calcularDistanciaHaversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
