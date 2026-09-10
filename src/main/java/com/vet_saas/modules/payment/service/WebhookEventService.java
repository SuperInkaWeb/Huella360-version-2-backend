package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import com.vet_saas.modules.payment.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventService {

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
                .nextRetryAt(LocalDateTime.now())
                .build();
        return webhookEventRepository.save(event);
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
