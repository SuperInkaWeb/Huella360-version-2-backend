package com.vet_saas.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Auth0JwtDecoderTest {

    private static final String HMAC_SECRET = "VGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgMTIzNDU2Nzg5";
    private Auth0JwtDecoder decoder;
    private SecretKey hmacKey;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = Base64.getDecoder().decode(HMAC_SECRET);
        hmacKey = Keys.hmacShaKeyFor(secretBytes);
        decoder = new Auth0JwtDecoder("https://dev-zg0aldkj2slve32o.us.auth0.com/", HMAC_SECRET);
    }

    @Test
    void decodeLegacyToken_HS256_returnsJwt() {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "EMPRESA")
                .claim("correo", "test@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(hmacKey)
                .compact();

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertEquals("42", jwt.getSubject());
        assertEquals("EMPRESA", jwt.getClaimAsString("role"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    @Test
    void decodeLegacyToken_withHmacSecret_textPlain() {
        String plainSecret = "This-is-a-secret-key-for-testing-12345678";
        Auth0JwtDecoder plainDecoder = new Auth0JwtDecoder("https://example.com/", plainSecret);

        SecretKey key = Keys.hmacShaKeyFor(plainSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("99")
                .claim("role", "CLIENTE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        Jwt jwt = plainDecoder.decode(token);

        assertNotNull(jwt);
        assertEquals("99", jwt.getSubject());
        assertEquals("CLIENTE", jwt.getClaimAsString("role"));
    }

    @Test
    void decodeLegacyToken_invalidSignature_throwsJwtException() {
        String otherSecret = "VGhpcyBpcyBhbm90aGVyIHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgMTIzNDU2Nzg5";
        byte[] otherBytes = Base64.getDecoder().decode(otherSecret);
        SecretKey otherKey = Keys.hmacShaKeyFor(otherBytes);

        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey)
                .compact();

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void decodeLegacyToken_expired_throwsJwtException() {
        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(hmacKey)
                .compact();

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void decode_malformedToken_throwsJwtException() {
        assertThrows(JwtException.class, () -> decoder.decode("not.a.valid.jwt"));
    }

    @Test
    void decode_nullToken_throwsException() {
        assertThrows(Exception.class, () -> decoder.decode(null));
    }

    @Test
    void decodeLegacyToken_preservesAllClaims() {
        String token = Jwts.builder()
                .subject("7")
                .claim("role", "VETERINARIO")
                .claim("correo", "vet@test.com")
                .claim("empresaId", "15")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(hmacKey)
                .compact();

        Jwt jwt = decoder.decode(token);

        assertEquals("7", jwt.getSubject());
        assertEquals("VETERINARIO", jwt.getClaimAsString("role"));
        assertEquals("vet@test.com", jwt.getClaimAsString("correo"));
        assertEquals("15", jwt.getClaimAsString("empresaId"));
    }

    @Test
    void constructor_blankSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Auth0JwtDecoder("https://example.com/", "   "));
    }

    @Test
    void constructor_nullSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Auth0JwtDecoder("https://example.com/", null));
    }

    @Test
    void constructor_tooShortSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Auth0JwtDecoder("https://example.com/", "short"));
    }
}
