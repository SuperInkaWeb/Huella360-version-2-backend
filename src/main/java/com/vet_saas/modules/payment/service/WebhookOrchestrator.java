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
    private final AppProperties appProperties;

    @Async("webhookExecutor")
    public void processWebhookAsync(String paymentId, String pathEmpresaId) {
        LOGGER.info("Iniciando procesamiento asíncrono de webhook. paymentId: {}", paymentId);

        String tokenToUse = determineTokenToUse(pathEmpresaId);

        Payment payment = mpGateway.getPaymentDetails(paymentId, tokenToUse);
        Map<String, Object> metadata = payment.getMetadata();

        if (metadata == null) {
            LOGGER.error("El pago {} no contiene metadata", paymentId);
            return;
        }

        paymentService.processPaymentDatabaseTransaction(payment, metadata, pathEmpresaId);
    }

    private String determineTokenToUse(String pathEmpresaId) {
        String token = appProperties.getExternal().getMercadoPago().getAccessToken();
        if (token == null || token.isBlank()) {
            throw new BusinessException("La plataforma no tiene configurada su pasarela de pagos.");
        }
        return token;
    }
}