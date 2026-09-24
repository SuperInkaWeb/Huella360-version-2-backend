package com.vet_saas.modules.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Valida la firma de las notificaciones de Mercado Pago según su formato oficial:
 * header {@code x-signature: ts=<ts>,v1=<hmac>} y HMAC-SHA256 (hex) del manifiesto
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}, usando la clave secreta
 * del webhook configurada en el panel de Mercado Pago.
 */
@Component
public class MercadoPagoWebhookSignatureValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoWebhookSignatureValidator.class);

    public boolean isValid(String secret, String xSignature, String xRequestId, String dataId) {
        if (secret == null || secret.isBlank()) {
            LOGGER.warn("CRITICAL: No webhook secret configured (MP_WEBHOOK_SECRET). Rejecting webhook");
            return false;
        }
        if (xSignature == null || xSignature.isBlank()) {
            LOGGER.warn("[QA-DIAG] webhook sin x-signature, x-request-id={} dataId={}", xRequestId, dataId);
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("ts".equals(kv[0].trim())) ts = kv[1].trim();
            else if ("v1".equals(kv[0].trim())) v1 = kv[1].trim();
        }
        if (ts == null || ts.isEmpty() || v1 == null || v1.isEmpty()) {
            return false;
        }

        String manifest = buildManifest(dataId, xRequestId, ts);
        try {
            String expected = hmacSha256Hex(secret, manifest);
            boolean ok = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            // TODO(QA-DIAG): log temporal solo en la rama de QA, no va al PR. No imprime el secret.
            LOGGER.warn("[QA-DIAG] webhook firma ok={} manifest='{}' v1Recibido={} v1Calculado={} secretLen={} secretTrim={}",
                    ok, manifest, v1, expected, secret.length(), secret.strip().length() == secret.length());
            return ok;
        } catch (Exception e) {
            // Intentional: any validation error results in rejection
            LOGGER.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Mercado Pago indica omitir del manifiesto cualquier valor que no venga en la notificación,
     * y usar data.id en minúsculas cuando es alfanumérico.
     */
    String buildManifest(String dataId, String xRequestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId).append(';');
        }
        manifest.append("ts:").append(ts).append(';');
        return manifest.toString();
    }

    private String hmacSha256Hex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
