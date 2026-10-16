package com.vet_saas.modules.payment.service;

import com.vet_saas.modules.payment.model.WebhookEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(webhookEventService.tomarAvisoDePendientes()).thenReturn(true);
        when(webhookEventService.findRetryableEvents()).thenReturn(List.of(e1, e2));

        scheduler.retryFailedWebhooks();

        verify(webhookOrchestrator).process(1L, "111", null);
        verify(webhookOrchestrator).process(2L, "222", "5");
        verify(webhookEventService).revisarSiQuedanPendientes();
    }

    @Test
    void sinEventosPendientes_noHaceNada() {
        when(webhookEventService.tomarAvisoDePendientes()).thenReturn(true);
        when(webhookEventService.findRetryableEvents()).thenReturn(List.of());

        scheduler.retryFailedWebhooks();

        verifyNoInteractions(webhookOrchestrator);
        verify(webhookEventService).revisarSiQuedanPendientes();
    }

    @Test
    void sinAvisoDePendientes_noConsultaLaBd() {
        // Sin esto el job consultaba la BD cada minuto y Neon nunca se apagaba.
        when(webhookEventService.tomarAvisoDePendientes()).thenReturn(false);

        scheduler.retryFailedWebhooks();

        verify(webhookEventService, never()).findRetryableEvents();
        verify(webhookEventService, never()).revisarSiQuedanPendientes();
        verifyNoInteractions(webhookOrchestrator);
    }

    @Test
    void siUnReintentoFalla_igualRevisaSiQuedanPendientes() {
        WebhookEvent e1 = WebhookEvent.builder().id(1L).paymentId("111").build();
        when(webhookEventService.tomarAvisoDePendientes()).thenReturn(true);
        when(webhookEventService.findRetryableEvents()).thenReturn(List.of(e1));
        doThrow(new RuntimeException("caida")).when(webhookOrchestrator).process(1L, "111", null);

        assertThatThrownBy(() -> scheduler.retryFailedWebhooks()).isInstanceOf(RuntimeException.class);

        verify(webhookEventService).revisarSiQuedanPendientes();
    }
}
