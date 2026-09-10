package com.vet_saas.modules.delivery.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.delivery.dto.request.RepartidorRequestDTO;
import com.vet_saas.modules.delivery.dto.request.UbicacionDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.RepartidorResponseDTO;
import com.vet_saas.modules.delivery.mapper.DeliveryMapper;
import com.vet_saas.modules.delivery.model.DeliveryStatus;
import com.vet_saas.modules.delivery.model.Repartidor;
import com.vet_saas.modules.delivery.model.RepartidorStatus;
import com.vet_saas.modules.delivery.repository.RepartidorRepository;
import com.vet_saas.modules.delivery.repository.DeliveryRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RepartidorService {

    private final RepartidorRepository repartidorRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final UsuarioRepository usuarioRepository;

    public RepartidorResponseDTO createProfile(Long usuarioId, RepartidorRequestDTO dto, String fotoUrl) {
        Optional<Repartidor> existing = repartidorRepository.findByUsuarioId(usuarioId);
        if (existing.isPresent()) {
            log.info("Perfil de repartidor ya existe para usuarioId {}, procediendo a actualizar en lugar de crear", usuarioId);
            return updateProfile(usuarioId, dto, fotoUrl);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Repartidor repartidor = Repartidor.builder()
                .usuario(usuario)
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .telefono(dto.getTelefono())
                .tipoVehiculo(dto.getTipoVehiculo())
                .placaVehiculo(dto.getPlacaVehiculo())
                .fotoPerfil(fotoUrl)
                .build();

        return toDTO(repartidorRepository.save(repartidor));
    }

    public RepartidorResponseDTO updateProfile(Long usuarioId, RepartidorRequestDTO dto, String fotoUrl) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado"));

        repartidor.setNombres(dto.getNombres());
        repartidor.setApellidos(dto.getApellidos());
        repartidor.setTelefono(dto.getTelefono());
        repartidor.setTipoVehiculo(dto.getTipoVehiculo());
        repartidor.setPlacaVehiculo(dto.getPlacaVehiculo());
        
        if (fotoUrl != null) {
            repartidor.setFotoPerfil(fotoUrl);
        }

        return toDTO(repartidorRepository.save(repartidor));
    }

    @Transactional(readOnly = true)
    public RepartidorResponseDTO getByUsuarioId(Long usuarioId) {
        Repartidor r = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de repartidor no encontrado"));
        return toDTO(r);
    }

    /** El repartidor activa/desactiva su disponibilidad */
    public void cambiarDisponibilidad(Long usuarioId, boolean disponible) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));

        if (disponible && repartidor.getEstadoActual() == RepartidorStatus.OCUPADO) {
            throw new BusinessException("No puedes activarte mientras tienes un delivery activo");
        }

        RepartidorStatus nuevoEstado = disponible ? RepartidorStatus.DISPONIBLE : RepartidorStatus.OFFLINE;
        repartidorRepository.actualizarEstado(repartidor.getIdRepartidor(), nuevoEstado);
        log.info("Repartidor {} cambió a {}", repartidor.getIdRepartidor(), nuevoEstado);
    }

    /** Actualiza ubicacion GPS del repartidor */
    public void actualizarUbicacion(Long usuarioId, UbicacionDTO dto) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));

        repartidorRepository.actualizarUbicacion(
            repartidor.getIdRepartidor(),
            dto.getLat(),
            dto.getLng()
        );
    }

    /** Deliveries activos del repartidor (hasta maxPedidosSimultaneos en curso) */
    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getDeliveriesActivos(Long usuarioId) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));

        return deliveryRepository.findByRepartidorIdRepartidorAndEstadoNotIn(
            repartidor.getIdRepartidor(),
            List.of(DeliveryStatus.ENTREGADO, DeliveryStatus.CANCELADO, DeliveryStatus.FALLIDO, DeliveryStatus.INCIDENCIA)
        ).stream().map(deliveryMapper::toResponseDTO).toList();
    }

    /** Historial de entregas completadas del repartidor */
    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getHistorial(Long usuarioId) {
        Repartidor repartidor = repartidorRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));

        return deliveryRepository
            .findByRepartidorIdRepartidorAndEstadoOrderByCreatedAtDesc(
                repartidor.getIdRepartidor(), DeliveryStatus.ENTREGADO)
            .stream()
            .map(deliveryMapper::toResponseDTO)
            .toList();
    }

    private RepartidorResponseDTO toDTO(Repartidor r) {
        return RepartidorResponseDTO.builder()
            .idRepartidor(r.getIdRepartidor())
            .nombres(r.getNombres())
            .apellidos(r.getApellidos())
            .telefono(r.getTelefono())
            .fotoPerfil(r.getFotoPerfil())
            .tipoVehiculo(r.getTipoVehiculo())
            .placaVehiculo(r.getPlacaVehiculo())
            .estadoActual(r.getEstadoActual())
            .estadoValidacion(r.getEstadoValidacion())
            .calificacionPromedio(r.getCalificacionPromedio())
            .totalEntregas(r.getTotalEntregas())
            .ubicacionLat(r.getUbicacionLat())
            .ubicacionLng(r.getUbicacionLng())
            .ultimaUbicacionAt(r.getUltimaUbicacionAt())
            .activo(r.getActivo())
            .build();
    }
}
