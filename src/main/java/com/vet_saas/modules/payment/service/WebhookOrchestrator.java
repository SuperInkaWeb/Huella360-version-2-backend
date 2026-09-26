package com.vet_saas.modules.payment.service;

import com.mercadopago.resources.payment.Payment;
import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebhookOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookOrchestrator.class);

    private final MercadoPagoGateway mpGateway;
    private final PaymentService paymentService;
    private final WebhookEventService webhookEventService;
    private final AppProperties appProperties;

    @Async("webhookExecutor")
    public void processWebhookAsync(Long eventId, String paymentId, String pathEmpresaId) {
        LOGGER.info("Iniciando procesamiento asíncrono de webhook. paymentId: {}", paymentId);
        process(eventId, paymentId, pathEmpresaId);
    }

    /**
     * Procesa un evento de webhook y deja constancia del resultado en webhook_events:
     * COMPLETED si se aplico, FAILED (con backoff) si fallo, para que WebhookRetryScheduler
     * lo reintente. Antes nadie actualizaba el estado y un fallo aqui perdia el pago:
     * Mercado Pago ya habia recibido 200 y no reintenta.
     */
    public void process(Long eventId, String paymentId, String pathEmpresaId) {
        try {
            String tokenToUse = determineTokenToUse(pathEmpresaId);

            Payment payment = mpGateway.getPaymentDetails(paymentId, tokenToUse);
            Map<String, Object> metadata = payment.getMetadata();

            if (metadata == null) {
                throw new BusinessException("El pago " + paymentId + " no contiene metadata");
            }

            paymentService.processPaymentDatabaseTransaction(payment, metadata, pathEmpresaId);
            webhookEventService.markCompleted(paymentId);
            LOGGER.info("Webhook procesado. paymentId: {}", paymentId);
        } catch (Exception ex) {
            LOGGER.error("Error procesando webhook. eventId: {} paymentId: {}", eventId, paymentId, ex);
            webhookEventService.markFailed(eventId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String determineTokenToUse(String pathEmpresaId) {
        String token = appProperties.getExternal().getMercadoPago().getAccessToken();
        if (token == null || token.isBlank()) {
            throw new BusinessException("La plataforma no tiene configurada su pasarela de pagos.");
        }
        return token;
    }
}
