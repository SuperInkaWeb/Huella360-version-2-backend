package com.vet_saas.core.utils;

import com.vet_saas.core.exceptions.types.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    private static final String VALID_BASE64_KEY = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0NTY=";

    private final CryptoUtil cryptoUtil = new CryptoUtil(VALID_BASE64_KEY);

    @Test
    void encrypt_andDecrypt_roundtrip() {
        String original = "super-secret-mp-access-token";
        String encrypted = cryptoUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = cryptoUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        assertNull(cryptoUtil.encrypt(null));
    }

    @Test
    void encrypt_blankInput_returnsNull() {
        assertNull(cryptoUtil.encrypt("   "));
    }

    @Test
    void decrypt_nullInput_returnsNull() {
        assertNull(cryptoUtil.decrypt(null));
    }

    @Test
    void decrypt_blankInput_returnsNull() {
        assertNull(cryptoUtil.decrypt("   "));
    }

    @Test
    void encrypt_differentOutputs_eachTime() {
        String original = "consistent-plain-text";
        String enc1 = cryptoUtil.encrypt(original);
        String enc2 = cryptoUtil.encrypt(original);
        assertNotEquals(enc1, enc2);
        assertEquals(original, cryptoUtil.decrypt(enc1));
        assertEquals(original, cryptoUtil.decrypt(enc2));
    }

    @Test
    void decrypt_tamperedData_throwsBusinessException() {
        String encrypted = cryptoUtil.encrypt("some-value");
        assertNotNull(encrypted);
        String tampered = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";
        assertThrows(BusinessException.class, () -> cryptoUtil.decrypt(tampered));
    }

    @Test
    void constructor_blankSecret_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> new CryptoUtil(""));
        assertThrows(IllegalStateException.class, () -> new CryptoUtil(null));
    }

    @Test
    void constructor_invalidBase64_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new CryptoUtil("not-valid-base64!!!"));
    }

    @Test
    void constructor_wrongKeyLength_throwsIllegalArgument() {
        String shortKey = java.util.Base64.getEncoder().encodeToString("short".getBytes());
        assertThrows(IllegalArgumentException.class, () -> new CryptoUtil(shortKey));
    }
}
