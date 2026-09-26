package com.vet_saas.modules.payment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.payment.dto.PaymentPreferenceResponse;
import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.payment.service.MercadoPagoWebhookSignatureValidator;
import com.vet_saas.modules.payment.service.PaymentService;
import com.vet_saas.modules.payment.service.WebhookEventService;
import com.vet_saas.modules.payment.service.WebhookOrchestrator;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final WebhookOrchestrator webhookOrchestrator;
    private final WebhookEventService webhookEventService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final MercadoPagoWebhookSignatureValidator signatureValidator;

    @PostMapping("/checkout/{orderId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'EMPRESA', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<PaymentPreferenceResponse>> generatePaymentLink(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Usuario usuarioActual) {

        PaymentPreferenceResponse response = paymentService.createCheckoutUrl(orderId, usuarioActual);

        LOGGER.info("Checkout link generated orderId={}", orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Link de pago generado exitosamente"));
    }

    @PostMapping("/checkout/guest/{orderId}")
    public ResponseEntity<ApiResponse<PaymentPreferenceResponse>> generateGuestPaymentLink(
            @PathVariable Long orderId) {

        PaymentPreferenceResponse response = paymentService.createGuestCheckoutUrl(orderId);

        LOGGER.info("Guest checkout link generated orderId={}", orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Link de pago generado exitosamente"));
    }

    @PostMapping("/webhook/{empresaId}")
    public ResponseEntity<Void> receiveWebhook(
            @PathVariable String empresaId,
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {
        Map<String, Object> body = parseRawBody(rawBody);
        if (!isValidWebhookSignature(request, queryParams, body)) {
            LOGGER.warn("Webhook signature validation failed from {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return handleWebhook(empresaId, queryParams, body);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receivePlatformWebhook(
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {
        Map<String, Object> body = parseRawBody(rawBody);
        if (!isValidWebhookSignature(request, queryParams, body)) {
            LOGGER.warn("Platform webhook signature validation failed from {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return handleWebhook(null, queryParams, body);
    }

    private boolean isValidWebhookSignature(HttpServletRequest request, Map<String, String> queryParams,
                                            Map<String, Object> body) {
        String webhookSecret = appProperties.getExternal().getMercadoPago().getWebhookSecret();
        String dataId = queryParams.get("data.id");
        if (dataId == null) dataId = extractPaymentId(Map.of(), body);
        return signatureValidator.isValid(
                webhookSecret,
                request.getHeader("x-signature"),
                request.getHeader("x-request-id"),
                dataId);
    }

    private Map<String, Object> parseRawBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return null;
        try {
            return objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            LOGGER.error("Error parsing webhook body", e);
            return null;
        }
    }

    private ResponseEntity<Void> handleWebhook(
            String pathEmpresaId,
            Map<String, String> queryParams,
            Map<String, Object> body) {

        String id = extractPaymentId(queryParams, body);

        if (id == null) {
            LOGGER.warn("Webhook sin ID. Query: {}, Body: {}", queryParams, body);
            return ResponseEntity.ok().build();
        }

        String type = queryParams.get("type");
        String topic = queryParams.get("topic");

        if ("payment".equals(type) || "payment".equals(topic)) {
            Long eventId = webhookEventService.saveEvent(id, pathEmpresaId).getId();
            webhookOrchestrator.processWebhookAsync(eventId, id, pathEmpresaId);
        }

        return ResponseEntity.ok().build();
    }

    private String extractPaymentId(Map<String, String> queryParams, Map<String, Object> body) {
        String id = queryParams.get("data.id");
        if (id == null) id = queryParams.get("id");
        if (id == null && body != null && body.containsKey("data")) {
            Object dataObj = body.get("data");
            if (dataObj instanceof Map data) {
                if (data.containsKey("id")) {
                    id = data.get("id").toString();
                }
            }
        }
        return id;
    }

    @GetMapping("/sync")
    @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> syncPayment(
            @RequestParam("payment_id") String paymentId,
            @RequestParam("external_reference") String codigoOrden) {
        paymentService.syncPaymentStatus(paymentId, codigoOrden);
        return ResponseEntity.ok(ApiResponse.success(null, "Pago sincronizado correctamente"));
    }
}