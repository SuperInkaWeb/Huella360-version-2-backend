package com.vet_saas.modules.payment.service;

import com.mercadopago.resources.payment.Payment;
import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * H360-PAY: antes nadie actualizaba webhook_events y un fallo al procesar perdia el pago
 * (Mercado Pago ya habia recibido 200 y no reintenta). Ahora cada intento deja COMPLETED
 * o FAILED para que WebhookRetryScheduler lo reintente.
 */
@ExtendWith(MockitoExtension.class)
class WebhookOrchestratorTest {

    @Mock private MercadoPagoGateway mpGateway;
    @Mock private PaymentService paymentService;
    @Mock private WebhookEventService webhookEventService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AppProperties appProperties;

    private WebhookOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new WebhookOrchestrator(mpGateway, paymentService, webhookEventService, appProperties);
        lenient().when(appProperties.getExternal().getMercadoPago().getAccessToken()).thenReturn("TEST-token");
    }

    @Test
    void process_exitoso_marcaCompleted() {
        Payment payment = mock(Payment.class);
        Map<String, Object> metadata = Map.of("type", "SUBSCRIPTION");
        when(payment.getMetadata()).thenReturn(metadata);
        when(mpGateway.getPaymentDetails("555", "TEST-token")).thenReturn(payment);

        orchestrator.process(10L, "555", null);

        verify(paymentService).processPaymentDatabaseTransaction(payment, metadata, null);
        verify(webhookEventService).markCompleted("555");
        verify(webhookEventService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void process_falloAlConsultarMercadoPago_marcaFailedParaReintentar() {
        when(mpGateway.getPaymentDetails("555", "TEST-token")).thenThrow(new RuntimeException("timeout"));

        orchestrator.process(10L, "555", null);

        verify(webhookEventService).markFailed(eq(10L), contains("timeout"));
        verify(webhookEventService, never()).markCompleted(any());
        verifyNoInteractions(paymentService);
    }

    @Test
    void process_falloAlAplicarElPago_marcaFailed() {
        Payment payment = mock(Payment.class);
        when(payment.getMetadata()).thenReturn(Map.of("type", "SUBSCRIPTION"));
        when(mpGateway.getPaymentDetails("555", "TEST-token")).thenReturn(payment);
        doThrow(new IllegalStateException("db caida"))
                .when(paymentService).processPaymentDatabaseTransaction(any(), any(), any());

        orchestrator.process(10L, "555", null);

        verify(webhookEventService).markFailed(eq(10L), contains("db caida"));
        verify(webhookEventService, never()).markCompleted(any());
    }

    @Test
    void process_pagoSinMetadata_marcaFailedSinProcesar() {
        Payment payment = mock(Payment.class);
        when(payment.getMetadata()).thenReturn(null);
        when(mpGateway.getPaymentDetails("555", "TEST-token")).thenReturn(payment);

        orchestrator.process(10L, "555", null);

        verify(webhookEventService).markFailed(eq(10L), contains("metadata"));
        verifyNoInteractions(paymentService);
    }
}
