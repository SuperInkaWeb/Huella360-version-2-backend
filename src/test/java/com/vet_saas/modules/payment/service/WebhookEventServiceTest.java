package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import com.vet_saas.modules.payment.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @Mock private WebhookEventRepository webhookEventRepository;
    @InjectMocks private WebhookEventService webhookEventService;

    @Test
    void saveEvent_noEsReintentableHastaPasarElMargenDelPrimerIntento() {
        // El primer intento corre async al recibir el webhook; si el job lo tomara de inmediato
        // procesaria el mismo evento dos veces en paralelo.
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now();

        WebhookEvent event = webhookEventService.saveEvent("555", null);

        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getNextRetryAt())
                .isAfterOrEqualTo(antes.plusMinutes(WebhookEventService.INITIAL_RETRY_GRACE_MINUTES));
    }
}
