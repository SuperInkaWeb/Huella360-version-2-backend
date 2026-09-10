package com.vet_saas.modules.payment.repository;

import com.vet_saas.modules.payment.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    @Query("SELECT we FROM WebhookEvent we WHERE we.status IN ('PENDING', 'FAILED') " +
           "AND we.attempts < we.maxAttempts AND we.nextRetryAt <= :now")
    List<WebhookEvent> findRetryableEvents(LocalDateTime now);

    @Modifying
    @Query("UPDATE WebhookEvent we SET we.status = 'COMPLETED' WHERE we.paymentId = :paymentId AND we.status != 'COMPLETED'")
    int markCompletedByPaymentId(String paymentId);

    @Modifying
    @Query("UPDATE WebhookEvent we SET we.status = 'FAILED', we.attempts = we.attempts + 1, " +
           "we.lastError = :error, we.nextRetryAt = :nextRetry " +
           "WHERE we.id = :id")
    void markFailed(Long id, String error, LocalDateTime nextRetry);
}
