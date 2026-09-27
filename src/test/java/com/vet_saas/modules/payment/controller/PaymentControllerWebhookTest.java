package com.vet_saas.modules.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.payment.model.WebhookEvent;
import com.vet_saas.modules.payment.service.MercadoPagoWebhookSignatureValidator;
import com.vet_saas.modules.payment.service.PaymentService;
import com.vet_saas.modules.payment.service.WebhookEventService;
import com.vet_saas.modules.payment.service.WebhookOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Revision de Alexis (PR #8): cubre como PaymentController obtiene data.id (query o body),
 * x-signature y x-request-id y los entrega al validador, y que solo un webhook con firma valida
 * continua el procesamiento. Usa el MercadoPagoWebhookSignatureValidator REAL (no un mock); las
 * firmas esperadas se calculan en el test armando el manifiesto oficial de Mercado Pago a mano
 * ("id:<data.id>;request-id:<x-request-id>;ts:<ts>;"), sin reutilizar codigo del validador.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerWebhookTest {

    private static final String SECRET = "secret-webhook-qa";
    private static final String REQUEST_ID = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
    private static final String TS = "1790264577";

    @Mock private PaymentService paymentService;
    @Mock private WebhookOrchestrator webhookOrchestrator;
    @Mock private WebhookEventService webhookEventService;
    @Mock private AppProperties appProperties;

    private AppProperties.External external;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        external = new AppProperties.External();
        external.getMercadoPago().setWebhookSecret(SECRET);
        lenient().when(appProperties.getExternal()).thenReturn(external);
        controller = new PaymentController(paymentService, webhookOrchestrator, webhookEventService,
                appProperties, new ObjectMapper(), new MercadoPagoWebhookSignatureValidator());
    }

    // --- helpers -------------------------------------------------------------------------------

    private static String hmac(String secret, String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }

    /** x-signature tal como la envia Mercado Pago, firmada con el manifiesto oficial. */
    private static String firmaValida(String dataId) throws Exception {
        String manifest = "id:" + dataId + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";";
        return "ts=" + TS + ",v1=" + hmac(SECRET, manifest);
    }

    private static MockHttpServletRequest request(String xSignature, String xRequestId) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/payments/webhook");
        if (xSignature != null) req.addHeader("x-signature", xSignature);
        if (xRequestId != null) req.addHeader("x-request-id", xRequestId);
        return req;
    }

    private static String body(String dataId) {
        return "{\"action\":\"payment.created\",\"type\":\"payment\",\"data\":{\"id\":\"" + dataId + "\"}}";
    }

    private void verificarQueNoSeProceso() {
        verifyNoInteractions(webhookEventService, webhookOrchestrator);
    }

    // --- aceptados -----------------------------------------------------------------------------

    @Test
    void dataIdEnQuery_yHeadersValidos_seAceptaYContinuaElProcesamiento() throws Exception {
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");
        when(webhookEventService.saveEvent("180663306524", null)).thenReturn(WebhookEvent.builder().id(11L).build());

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request(firmaValida("180663306524"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookEventService).saveEvent("180663306524", null);
        verify(webhookOrchestrator).processWebhookAsync(11L, "180663306524", null);
    }

    @Test
    void sinDataIdEnQuery_peroPresenteEnElBody_usaElFallbackParaValidarYProcesar() throws Exception {
        // La firma se calcula con el data.id del body: si el controller no usara el fallback,
        // el manifiesto no tendria "id:..." y la firma no coincidiria (503).
        Map<String, String> query = Map.of("type", "payment");
        when(webhookEventService.saveEvent("777000111", null)).thenReturn(WebhookEvent.builder().id(12L).build());

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("777000111"),
                request(firmaValida("777000111"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookEventService).saveEvent("777000111", null);
        verify(webhookOrchestrator).processWebhookAsync(12L, "777000111", null);
    }

    @Test
    void endpointPorEmpresa_conFirmaValida_procesaConElEmpresaIdDelPath() throws Exception {
        Map<String, String> query = Map.of("data.id", "555", "type", "payment");
        when(webhookEventService.saveEvent("555", "3")).thenReturn(WebhookEvent.builder().id(13L).build());

        ResponseEntity<Void> r = controller.receiveWebhook("3", query, body("555"),
                request(firmaValida("555"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookEventService).saveEvent("555", "3");
        verify(webhookOrchestrator).processWebhookAsync(13L, "555", "3");
    }

    // --- rechazados ----------------------------------------------------------------------------

    @Test
    void firmaInvalida_responde503_yNoContinuaElProcesamiento() throws Exception {
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");
        String firmaDeOtroSecret = "ts=" + TS + ",v1=" + hmac("otro-secret", "id:180663306524;request-id:" + REQUEST_ID + ";ts:" + TS + ";");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request(firmaDeOtroSecret, REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void firmaDeOtroPago_responde503() throws Exception {
        // Firma valida de otro data.id reutilizada con este pago (replay manipulado)
        Map<String, String> query = Map.of("data.id", "999", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("999"),
                request(firmaValida("180663306524"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void secretAusente_rechaza_yNoProcesa() throws Exception {
        external.getMercadoPago().setWebhookSecret(null);
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request(firmaValida("180663306524"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void secretEnBlanco_rechaza_yNoProcesa() throws Exception {
        external.getMercadoPago().setWebhookSecret("   ");
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request(firmaValida("180663306524"), REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void sinHeaderXSignature_rechaza_yNoProcesa() {
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"), request(null, REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void xSignatureIncompleto_sinV1_rechaza_yNoProcesa() {
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request("ts=" + TS, REQUEST_ID));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }

    @Test
    void faltaXRequestId_conFirmaQueLoIncluia_rechaza_yNoProcesa() throws Exception {
        // El controller debe pasar el header x-request-id al validador: sin el, el manifiesto cambia
        Map<String, String> query = Map.of("data.id", "180663306524", "type", "payment");

        ResponseEntity<Void> r = controller.receivePlatformWebhook(query, body("180663306524"),
                request(firmaValida("180663306524"), null));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verificarQueNoSeProceso();
    }
}
