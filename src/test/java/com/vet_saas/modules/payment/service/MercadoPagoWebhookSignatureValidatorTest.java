package com.vet_saas.modules.payment.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las firmas esperadas se calcularon fuera de Java (Python hmac/hashlib) con el formato oficial
 * de Mercado Pago, para no validar la implementación contra sí misma.
 */
class MercadoPagoWebhookSignatureValidatorTest {

    private static final String SECRET = "test-webhook-secret";
    private static final String REQUEST_ID = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
    private static final String TS = "1704908010";
    // HMAC-SHA256("id:123456789;request-id:bb56a2f1-...;ts:1704908010;")
    private static final String VALID_V1 = "84a027b72f65253f3552243c5e627df84dfc2f20d4f0ec1d6f4cf7094dbd9594";

    private final MercadoPagoWebhookSignatureValidator validator = new MercadoPagoWebhookSignatureValidator();

    @Test
    void aceptaFirmaValidaConFormatoOficial() {
        assertThat(validator.isValid(SECRET, "ts=" + TS + ",v1=" + VALID_V1, REQUEST_ID, "123456789")).isTrue();
    }

    @Test
    void aceptaHeaderConEspaciosYOrdenInvertido() {
        assertThat(validator.isValid(SECRET, " v1=" + VALID_V1 + " , ts=" + TS, REQUEST_ID, "123456789")).isTrue();
    }

    @Test
    void usaDataIdEnMinusculasCuandoEsAlfanumerico() {
        String v1 = "3ac819bad25c320c75c0c3b0806ba85ce41198aa7c373a4fa3e589464768bdec";
        assertThat(validator.isValid(SECRET, "ts=" + TS + ",v1=" + v1, "req-1", "ABC123xyz")).isTrue();
    }

    @Test
    void omiteRequestIdDelManifiestoSiNoLlega() {
        String v1 = "26794e8d58abc40f4ad129c92922717a7e67ee9b8a128f93aacd5ecc2cb960ca";
        assertThat(validator.isValid(SECRET, "ts=" + TS + ",v1=" + v1, null, "123456789")).isTrue();
    }

    @Test
    void rechazaFirmaAlterada() {
        String tampered = "0" + VALID_V1.substring(1);
        assertThat(validator.isValid(SECRET, "ts=" + TS + ",v1=" + tampered, REQUEST_ID, "123456789")).isFalse();
    }

    @Test
    void rechazaSiCambiaElIdDelPago() {
        assertThat(validator.isValid(SECRET, "ts=" + TS + ",v1=" + VALID_V1, REQUEST_ID, "999999999")).isFalse();
    }

    @Test
    void rechazaConOtroSecret() {
        assertThat(validator.isValid("otro-secret", "ts=" + TS + ",v1=" + VALID_V1, REQUEST_ID, "123456789")).isFalse();
    }

    @Test
    void rechazaSinSecretConfigurado() {
        assertThat(validator.isValid("", "ts=" + TS + ",v1=" + VALID_V1, REQUEST_ID, "123456789")).isFalse();
        assertThat(validator.isValid(null, "ts=" + TS + ",v1=" + VALID_V1, REQUEST_ID, "123456789")).isFalse();
    }

    @Test
    void rechazaHeaderAusenteOIncompleto() {
        assertThat(validator.isValid(SECRET, null, REQUEST_ID, "123456789")).isFalse();
        assertThat(validator.isValid(SECRET, "v1=" + VALID_V1, REQUEST_ID, "123456789")).isFalse();
        assertThat(validator.isValid(SECRET, "ts=" + TS, REQUEST_ID, "123456789")).isFalse();
        assertThat(validator.isValid(SECRET, VALID_V1, REQUEST_ID, "123456789")).isFalse();
    }
}
