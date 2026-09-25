package com.vet_saas.modules.payment.service;

import com.mercadopago.resources.payment.Payment;
import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import com.vet_saas.modules.payment.repository.PagoRepository;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * H360-BUG: regresion para el bug encontrado en vivo el 23-09-2026 probando el
 * checkout real de Mercado Pago contra un ambiente desplegado (Render + Vercel):
 * un pago de SUSCRIPCION real, aprobado por Mercado Pago (verificado directamente
 * contra la API de MP: status=approved, S/49, metadata.type=SUBSCRIPTION), nunca
 * activaba el plan del cliente. La pantalla de retorno (PaymentSuccessPage) llama
 * a GET /payments/sync -> PaymentService.syncPaymentStatus(paymentId, codigoOrden),
 * que buscaba una Orden de marketplace de forma INCONDICIONAL antes de mirar el
 * tipo de pago. Como las suscripciones nunca crean una Orden, esto fallaba siempre
 * con "Orden no encontrada: SUBEMP-...", mostrandole al cliente un error aunque su
 * pago si se hubiera procesado correctamente.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AppProperties appProperties;
    @Mock private MercadoPagoGateway mpGateway;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private SubscriptionService subscriptionService;

    @Mock private Payment payment;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                ordenRepository, empresaRepository, pagoRepository, appProperties,
                mpGateway, eventPublisher, clienteRepository, veterinarioRepository,
                subscriptionService);

        AppProperties.External external = new AppProperties.External();
        external.getMercadoPago().setAccessToken("TEST-token");
        when(appProperties.getExternal()).thenReturn(external);
    }

    @Test
    void syncPaymentStatus_pagoDeSuscripcion_activaElPlanSinBuscarUnaOrden() {
        when(mpGateway.getPaymentDetails("179577069875", "TEST-token")).thenReturn(payment);
        when(payment.getMetadata()).thenReturn(Map.of(
                "type", "SUBSCRIPTION",
                "empresa_id", 1,
                "plan_id", 8));
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getId()).thenReturn(179577069875L);

        paymentService.syncPaymentStatus("179577069875", "SUBEMP-1-1790204973489");

        verify(subscriptionService).processSubscriptionPayment(1L, null, null, 8L, "179577069875");
        verify(ordenRepository, never()).findByCodigoOrden(anyString());
    }

    /**
     * Revision de Alexis (PR #7): el escenario rechazado no pudo validarse de punta a punta en el
     * sandbox de Mercado Pago. Un pago de SUSCRIPCION no aprobado debe seguir la rama de suscripcion
     * (sin buscar ninguna Orden: si fuera por la rama de ordenes fallaria con "Orden no encontrada")
     * y NO debe activar el plan (proteccion de handleSubscriptionWebhook).
     */
    @ParameterizedTest
    @ValueSource(strings = {"rejected", "cancelled", "pending", "in_process"})
    void syncPaymentStatus_pagoDeSuscripcionNoAprobado_noActivaElPlanNiBuscaOrden(String estadoMp) {
        when(mpGateway.getPaymentDetails("179656601317", "TEST-token")).thenReturn(payment);
        when(payment.getMetadata()).thenReturn(Map.of(
                "type", "SUBSCRIPTION",
                "empresa_id", 2,
                "plan_id", 9));
        when(payment.getStatus()).thenReturn(estadoMp);
        lenient().when(payment.getId()).thenReturn(179656601317L);

        // Procesado como SUBSCRIPTION: termina sin error (por la rama ORDER lanzaria "Orden no encontrada")
        assertDoesNotThrow(() -> paymentService.syncPaymentStatus("179656601317", "SUBEMP-2-1790264564625"));

        verify(subscriptionService, never()).processSubscriptionPayment(any(), any(), any(), any(), any());
        verifyNoInteractions(ordenRepository);
        verifyNoInteractions(pagoRepository, eventPublisher);
    }

    @Test
    void syncPaymentStatus_pagoDeOrden_siBuscaLaOrdenCorrespondiente() {
        Empresa empresa = Empresa.builder().id(5L).build();
        Orden orden = Orden.builder().id(10L).codigoOrden("ORD-5-999").empresa(empresa).build();

        when(mpGateway.getPaymentDetails("222", "TEST-token")).thenReturn(payment);
        when(payment.getMetadata()).thenReturn(Map.of(
                "type", "ORDER",
                "vendor_id", 5,
                "vendor_type", "EMPRESA"));
        when(payment.getExternalReference()).thenReturn("ORD-5-999");
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getId()).thenReturn(222L);
        when(payment.getTransactionAmount()).thenReturn(new java.math.BigDecimal("49.00"));
        when(ordenRepository.findByCodigoOrden("ORD-5-999")).thenReturn(Optional.of(orden));
        when(ordenRepository.findByCodigoOrdenForUpdate("ORD-5-999")).thenReturn(Optional.of(orden));
        when(pagoRepository.findByMpPaymentId("222")).thenReturn(Optional.empty());
        when(empresaRepository.findById(5L)).thenReturn(Optional.of(empresa));

        paymentService.syncPaymentStatus("222", "ORD-5-999");

        verify(ordenRepository).findByCodigoOrden("ORD-5-999");
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void syncPaymentStatus_sinMetadata_lanzaErrorClaro() {
        when(mpGateway.getPaymentDetails("333", "TEST-token")).thenReturn(payment);
        when(payment.getMetadata()).thenReturn(null);

        assertThrows(com.vet_saas.core.exceptions.types.BusinessException.class,
                () -> paymentService.syncPaymentStatus("333", "ALGO-999"));
        verify(ordenRepository, never()).findByCodigoOrden(anyString());
    }
}
