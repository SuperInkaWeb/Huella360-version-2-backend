package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRetrySchedulerTest {

    @Mock private WebhookEventService webhookEventService;
    @Mock private WebhookOrchestrator webhookOrchestrator;
    @InjectMocks private WebhookRetryScheduler scheduler;

    @Test
    void reintentaCadaEventoPendiente() {
        WebhookEvent e1 = WebhookEvent.builder().id(1L).paymentId("111").build();
        WebhookEvent e2 = WebhookEvent.builder().id(2L).paymentId("222").empresaId("5").build();
        when(webhookEventService.findRetryableEvents()).thenReturn(List.of(e1, e2));

        scheduler.retryFailedWebhooks();

        verify(webhookOrchestrator).process(1L, "111", null);
        verify(webhookOrchestrator).process(2L, "222", "5");
    }

    @Test
    void sinEventosPendientes_noHaceNada() {
        when(webhookEventService.findRetryableEvents()).thenReturn(List.of());

        scheduler.retryFailedWebhooks();

        verifyNoInteractions(webhookOrchestrator);
    }
}
