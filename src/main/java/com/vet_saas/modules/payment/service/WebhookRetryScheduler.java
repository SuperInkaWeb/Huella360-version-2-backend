package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reintenta los webhooks de Mercado Pago que fallaron (o que quedaron PENDING porque el
 * proceso se reinicio antes de terminarlos), con el backoff de WebhookEventService y hasta
 * max_attempts intentos.
 */
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookRetryScheduler.class);

    private final WebhookEventService webhookEventService;
    private final WebhookOrchestrator webhookOrchestrator;

    @Scheduled(fixedDelayString = "${app.webhooks.retry-interval-ms:60000}", initialDelay = 60000)
    public void retryFailedWebhooks() {
        // Sin webhooks nuevos ni fallidos no se toca la BD, para que Neon pueda apagarse.
        if (!webhookEventService.tomarAvisoDePendientes()) {
            return;
        }
        try {
            List<WebhookEvent> events = webhookEventService.findRetryableEvents();
            if (!events.isEmpty()) {
                LOGGER.info("Reintentando {} webhook(s) pendientes", events.size());
                for (WebhookEvent event : events) {
                    webhookOrchestrator.process(event.getId(), event.getPaymentId(), event.getEmpresaId());
                }
            }
        } finally {
            // Eventos aun en su margen de espera o con mas intentos por delante.
            webhookEventService.revisarSiQuedanPendientes();
        }
    }
}
