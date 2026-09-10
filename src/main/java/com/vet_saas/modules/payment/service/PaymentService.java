package com.vet_saas.modules.payment.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.resources.payment.Payment;
import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.payment.dto.PaymentPreferenceResponse;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import com.vet_saas.modules.payment.model.Pago;
import com.vet_saas.modules.payment.repository.PagoRepository;
import com.vet_saas.modules.sales.event.OrderPaidEvent;
import com.vet_saas.modules.sales.model.EstadoOrden;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final OrdenRepository ordenRepository;
    private final EmpresaRepository empresaRepository;
    private final PagoRepository pagoRepository;
    private final AppProperties appProperties;
    private final MercadoPagoGateway mpGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final ClienteRepository clienteRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final com.vet_saas.modules.subscription.service.SubscriptionService subscriptionService;

    @Transactional
    public PaymentPreferenceResponse createCheckoutUrl(Long ordenId, Usuario usuarioActual) {
        LOGGER.info("Iniciando generación de checkout para ordenId: {}", ordenId);

        Orden orden = ordenRepository.findByIdWithDetails(ordenId)
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));

        if (orden.getUsuarioCliente() == null || !orden.getUsuarioCliente().getId().equals(usuarioActual.getId())) {
            LOGGER.warn("Usuario {} intentó pagar la orden {} que no le pertenece", usuarioActual.getId(), ordenId);
            throw new ForbiddenException("No tienes permiso para procesar esta orden.");
        }

        return buildCheckout(orden);
    }

    @Transactional
    public PaymentPreferenceResponse createGuestCheckoutUrl(Long ordenId) {
        LOGGER.info("Iniciando generación de checkout guest para ordenId: {}", ordenId);

        Orden orden = ordenRepository.findByIdWithDetails(ordenId)
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));

        if (orden.getUsuarioCliente() != null) {
            throw new BusinessException("Esta orden pertenece a un usuario registrado. Usa el checkout normal.");
        }

        return buildCheckout(orden);
    }

    private PaymentPreferenceResponse buildCheckout(Orden orden) {
        LOGGER.info("Building checkout para ordenId: {}", orden.getId());

        if (orden.getEstado() != EstadoOrden.PENDIENTE) {
            LOGGER.warn("Abortando checkout: La orden {} ya está en estado {}", orden.getId(), orden.getEstado());
            throw new BusinessException("La orden ya fue procesada o no está disponible para pago");
        }

        Long vendorId;
        String vendorType;

        if (orden.getEmpresa() != null) {
            vendorId = orden.getEmpresa().getId();
            vendorType = "EMPRESA";
        } else if (orden.getVeterinario() != null) {
            vendorId = orden.getVeterinario().getId();
            vendorType = "VETERINARIO";
        } else {
            throw new BusinessException("La orden no tiene un vendedor (Empresa/Veterinario) asociado.");
        }

        String accessToken = appProperties.getExternal().getMercadoPago().getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.error("La plataforma no tiene configurado el Access Token de Mercado Pago");
            throw new BusinessException("La plataforma no tiene configurada su pasarela de pagos.");
        }

        // Resolve payer info: guest vs authenticated
        String payerName;
        String payerEmail;

        if (orden.getUsuarioCliente() != null) {
            com.vet_saas.modules.client.model.PerfilCliente perfil = clienteRepository
                    .findByUsuarioId(orden.getUsuarioCliente().getId())
                    .orElse(null);
            payerName = perfil != null ? perfil.getNombres() + " " + perfil.getApellidos() : "Cliente Huella360";
            payerEmail =orden.getUsuarioCliente().getCorreo();
        } else {
            payerName = orden.getGuestNombre() != null ? orden.getGuestNombre() : "Cliente Huella360";
            payerEmail = orden.getGuestEmail();
        }

        try {
            List<PreferenceItemRequest> items = new ArrayList<>(orden.getDetalles().stream()
                    .map(detalle -> PreferenceItemRequest.builder()
                            .id(detalle.getProducto().getId().toString())
                            .title(detalle.getProducto().getNombre())
                            .quantity(detalle.getCantidad())
                            .currencyId("PEN")
                            .unitPrice(detalle.getPrecioUnitario())
                            .build())
                    .toList());

            if (orden.getCostoEnvio() != null && orden.getCostoEnvio().compareTo(BigDecimal.ZERO) > 0) {
                items.add(PreferenceItemRequest.builder()
                        .id("SHIP-001")
                        .title("Costo de Envío")
                        .quantity(1)
                        .currencyId("PEN")
                        .unitPrice(orden.getCostoEnvio())
                        .build());
            }

            if (orden.getDescuento() != null && orden.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
                items.add(PreferenceItemRequest.builder()
                        .id("DESC-001")
                        .title("Descuento (Cupón)")
                        .quantity(1)
                        .currencyId("PEN")
                        .unitPrice(orden.getDescuento().negate())
                        .build());
            }

            boolean isSandbox = appProperties.getExternal().getMercadoPago().isSandbox();
            String effectivePayerEmail = null;

            if (isSandbox) {
                String configuredEmail = appProperties.getExternal().getMercadoPago().getSandboxBuyerEmail();
                if (configuredEmail != null && !configuredEmail.isBlank()) {
                    effectivePayerEmail = configuredEmail;
                    LOGGER.info("Sandbox detectado: usando email de prueba '{}' como payer", effectivePayerEmail);
                } else {
                    LOGGER.info("Sandbox detectado: dejando email en blanco para ingreso manual en el checkout");
                }
            } else {
                effectivePayerEmail = payerEmail;
            }

            PreferencePayerRequest.PreferencePayerRequestBuilder payerBuilder = PreferencePayerRequest.builder();
            if (effectivePayerEmail != null) {
                payerBuilder.email(effectivePayerEmail);
            }

            payerBuilder.name(payerName);

            PreferencePayerRequest payer = payerBuilder.build();

            String webhookBase = appProperties.getExternal().getBackendUrl();
            String notificationUrl = webhookBase + "/api/v1/payments/webhook";

            PaymentPreferenceResponse response = mpGateway.createPreference(
                    accessToken,
                    orden.getCodigoOrden(),
                    items,
                    payer,
                    Map.of(
                            "orden_id", orden.getId(),
                            "vendor_id", vendorId,
                            "vendor_type", vendorType),
                    appProperties.getExternal().getFrontendUrl() + "/marketplace/success",
                    notificationUrl);

            orden.setMpPreferenceId(response.preferenceId());
            ordenRepository.save(orden);

            LOGGER.info("Checkout generado exitosamente. preferenceId={} orden={}", response.preferenceId(),
                    orden.getCodigoOrden());

            return response;

        } catch (Exception ex) {
            LOGGER.error("Error creando preferencia para orden {}", orden.getId(), ex);
            throw new BusinessException("Error interno al procesar el pago");
        }
    }

    public void processWebhook(String paymentId, String pathEmpresaId) {
        LOGGER.info("Webhook recibido. paymentId: {} empresaId: {}", paymentId, pathEmpresaId);

        try {

            String tokenToUse = determineTokenToUse(pathEmpresaId);

            Payment payment = mpGateway.getPaymentDetails(paymentId, tokenToUse);
            Map<String, Object> metadata = payment.getMetadata();

            if (metadata == null) {
                LOGGER.error("El pago {} no contiene metadata", paymentId);
                return;
            }

            processPaymentDatabaseTransaction(payment, metadata, pathEmpresaId);

        } catch (Exception ex) {
            LOGGER.error("Error crítico al procesar el pago {}", paymentId, ex);
            throw ex;
        }
    }

    @Transactional
    public void syncPaymentStatus(String paymentId, String codigoOrden) {
        LOGGER.info("Sincronizando pago manualmente. paymentId: {}, codigoOrden: {}", paymentId, codigoOrden);
        Orden orden = ordenRepository.findByCodigoOrden(codigoOrden)
                .orElseThrow(() -> new BusinessException("Orden no encontrada: " + codigoOrden));

        String pathEmpresaId = orden.getEmpresa() != null ? orden.getEmpresa().getId().toString()
                : "vet_" + orden.getVeterinario().getId();

        processWebhook(paymentId, pathEmpresaId);
    }

    private void handleSubscriptionWebhook(Payment payment, Map<String, Object> metadata) {
        if (!"approved".equals(payment.getStatus())) {
            LOGGER.info("Pago de suscripción {} no aprobado (estado: {})", payment.getId(), payment.getStatus());
            return;
        }

        // Convertir de forma segura ya que MP puede enviar números como decimales
        // (ej:1.0)
        Long empresaId = metadata.containsKey("empresa_id")
                ? Double.valueOf(metadata.get("empresa_id").toString()).longValue()
                : null;
        Long veterinarioId = metadata.containsKey("veterinario_id")
                ? Double.valueOf(metadata.get("veterinario_id").toString()).longValue()
                : null;
        Long planId = Double.valueOf(metadata.get("plan_id").toString()).longValue();

        subscriptionService.processSubscriptionPayment(empresaId, veterinarioId, planId, payment.getId().toString());
    }

    private void handleOrderWebhook(Payment payment, Map<String, Object> metadata, String pathEmpresaId) {
        if (metadata.get("vendor_id") == null) {
            LOGGER.error("El pago {} no contiene metadata de vendor_id", payment.getId());
            return;
        }

        Long vendorId = ((Number) metadata.get("vendor_id")).longValue();
        String vendorType = metadata.get("vendor_type") != null ? metadata.get("vendor_type").toString() : "EMPRESA";

        Orden orden = ordenRepository.findByCodigoOrdenForUpdate(payment.getExternalReference())
                .orElseThrow(() -> new BusinessException("Orden no encontrada: " + payment.getExternalReference()));

        // Always validate tenant against the actual order data (after acquiring lock)
        String actualVendorKey;
        if (orden.getEmpresa() != null) {
            actualVendorKey = orden.getEmpresa().getId().toString();
        } else if (orden.getVeterinario() != null) {
            actualVendorKey = "vet_" + orden.getVeterinario().getId();
        } else {
            throw new BusinessException("La orden no tiene vendedor asociado");
        }

        String mpVendorKey = "VETERINARIO".equals(vendorType) ? "vet_" + vendorId : vendorId.toString();
        if (!mpVendorKey.equals(actualVendorKey)) {
            LOGGER.error("Cross-tenant webhook: mpVendorKey ({}) != actualVendorKey ({})", mpVendorKey, actualVendorKey);
            throw new ForbiddenException("El pago no pertenece a esta orden.");
        }

        String mpStatus = payment.getStatus();

        Pago pagoActual = pagoRepository.findByMpPaymentId(payment.getId().toString()).orElse(null);

        if (pagoActual != null) {
            if (pagoActual.getEstado().equals(mpStatus)) {
                LOGGER.info("Webhook ignorado. El pago {} ya fue procesado con el estado {}", payment.getId(), mpStatus);
                return;
            }

            LOGGER.info("Actualizando pago {} de estado {} a {}", payment.getId(), pagoActual.getEstado(), mpStatus);
            pagoActual.setEstado(mpStatus);
            pagoRepository.save(pagoActual);
        } else {

            Empresa empresa = "EMPRESA".equals(vendorType) ? empresaRepository.findById(vendorId).orElseThrow() : null;
            Veterinario veterinario = "VETERINARIO".equals(vendorType) ? veterinarioRepository.findById(vendorId).orElseThrow() : null;

            pagoActual = Pago.builder()
                    .empresa(empresa)
                    .veterinario(veterinario)
                    .orden(orden)
                    .mpPaymentId(payment.getId().toString())
                    .monto(payment.getTransactionAmount())
                    .metodoPago(payment.getPaymentMethodId())
                    .estado(mpStatus)
                    .build();
            pagoRepository.save(pagoActual);
        }

        EstadoOrden nuevoEstado = switch (mpStatus) {
            case "approved" -> EstadoOrden.PAGADO;
            case "rejected" -> EstadoOrden.FALLIDO;
            case "cancelled" -> EstadoOrden.CANCELADO;
            default -> orden.getEstado();
        };

        if (nuevoEstado != orden.getEstado()) {
            orden.setEstado(nuevoEstado);
            orden.setMetodoPago(payment.getPaymentMethodId());
            ordenRepository.save(orden);
            LOGGER.info("Orden {} actualizada al estado {}", orden.getCodigoOrden(), nuevoEstado);


            if (nuevoEstado == EstadoOrden.PAGADO) {
                eventPublisher.publishEvent(new OrderPaidEvent(this, orden));
            }
        }
    }

    private String determineTokenToUse(String pathEmpresaId) {
        String token = appProperties.getExternal().getMercadoPago().getAccessToken();
        if (token == null || token.isBlank()) {
            throw new BusinessException("La plataforma no tiene configurada su pasarela de pagos.");
        }
        return token;
    }

    @Transactional
    public void processPaymentDatabaseTransaction(Payment payment, Map<String, Object> metadata, String pathEmpresaId) {
        String type = metadata.get("type") != null ? metadata.get("type").toString() : "ORDER";

        if ("SUBSCRIPTION".equals(type)) {
            handleSubscriptionWebhook(payment, metadata);
        } else {
            handleOrderWebhook(payment, metadata, pathEmpresaId);
        }
    }
}