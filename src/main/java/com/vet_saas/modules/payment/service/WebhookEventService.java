package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import com.vet_saas.modules.payment.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventService {

    /**
     * El primer intento se hace en caliente (async) al recibir el webhook; el job de reintentos
     * solo toma el evento si sigue PENDING pasado este margen (p. ej. el proceso murio a mitad),
     * para no procesar el mismo evento dos veces en paralelo.
     */
    static final int INITIAL_RETRY_GRACE_MINUTES = 5;

    private final WebhookEventRepository webhookEventRepository;

    @Transactional
    public WebhookEvent saveEvent(String paymentId, String empresaId) {
        WebhookEvent event = WebhookEvent.builder()
                .paymentId(paymentId)
                .empresaId(empresaId)
                .status("PENDING")
                .attempts(0)
                .maxAttempts(5)
                .createdAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now().plusMinutes(INITIAL_RETRY_GRACE_MINUTES))
                .build();
        return webhookEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<WebhookEvent> findRetryableEvents() {
        return webhookEventRepository.findRetryableEvents(LocalDateTime.now());
    }

    @Transactional
    public void markCompleted(String paymentId) {
        webhookEventRepository.markCompletedByPaymentId(paymentId);
    }

    @Transactional
    public void markFailed(Long eventId, String error) {
        int nextDelayMinutes = calculateBackoff(eventId);
        webhookEventRepository.markFailed(eventId, error, LocalDateTime.now().plusMinutes(nextDelayMinutes));
    }

    private int calculateBackoff(Long eventId) {
        return webhookEventRepository.findById(eventId)
                .map(e -> Math.min(60, (int) Math.pow(2, e.getAttempts()) * 5))
                .orElse(5);
    }
}
