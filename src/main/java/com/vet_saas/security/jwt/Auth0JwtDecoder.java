package com.vet_saas.security.jwt;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

/**
 * JWT Decoder unificado para soportar dos tipos de tokens:
 *
 * 1. Tokens de Auth0 (RS256/RS384/RS512):
 *    - valida firma RSA mediante JWKS
 *    - valida issuer
 *    - valida audience
 *    - valida expiración mediante JJWT
 *
 * 2. Tokens legacy del backend (HS256/HS384/HS512):
 *    - valida firma HMAC mediante JWT_SECRET
 *    - valida expiración mediante JJWT
 *
 * El tipo de token se detecta inspeccionando el algoritmo del header JWT.
 */
@Slf4j
public class Auth0JwtDecoder implements JwtDecoder {

    private final String auth0IssuerUri;
    private final String auth0Audience;
    private final byte[] legacySecretBytes;

    // Cache de JWKS para evitar solicitar las claves en cada request.
    private volatile JWKSet cachedJwkSet;
    private volatile long lastFetchTime = 0;

    private static final long CACHE_TTL_MS = 3_600_000; // 1 hora

    public Auth0JwtDecoder(
            String auth0IssuerUri,
            String auth0Audience,
            String legacySecret
    ) {
        if (auth0IssuerUri == null || auth0IssuerUri.isBlank()) {
            throw new IllegalArgumentException(
                    "Auth0 issuer no puede estar vacío"
            );
        }

        if (auth0Audience == null || auth0Audience.isBlank()) {
            throw new IllegalArgumentException(
                    "Auth0 audience no puede estar vacío"
            );
        }

        this.auth0IssuerUri = auth0IssuerUri.trim();
        this.auth0Audience = auth0Audience.trim();
        this.legacySecretBytes = decodeSecret(legacySecret);
    }

    /**
     * Decodifica JWT_SECRET.
     *
     * Primero intenta interpretarlo como Base64.
     * Si no es Base64 válido, utiliza los bytes UTF-8 del texto.
     *
     * Se requieren al menos 32 bytes para HMAC-SHA256.
     */
    private byte[] decodeSecret(String secret) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret no puede estar vacío"
            );
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(secret);

            if (decoded.length >= 32) {
                return decoded;
            }

        } catch (IllegalArgumentException ignored) {
            // No es Base64 válido.
            // Se utilizará como texto plano.
        }

        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);

        if (raw.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret debe tener al menos 32 bytes (256 bits). Actual: "
                            + raw.length
            );
        }

        return raw;
    }

    @Override
    public Jwt decode(String token) throws JwtException {

        if (token == null || token.isBlank()) {
            throw new JwtException(
                    "Token JWT no puede estar vacío"
            );
        }

        try {

            String algorithm = extractAlgorithmFromHeader(token);

            log.debug(
                    "Decodificando JWT con algoritmo: {}",
                    algorithm
            );

            return switch (algorithm) {

                case "RS256", "RS384", "RS512" ->
                        decodeAuth0Token(token);

                case "HS256", "HS384", "HS512" ->
                        decodeLegacyToken(token);

                default ->
                        throw new JwtException(
                                "Algoritmo JWT no soportado: "
                                        + algorithm
                        );
            };

        } catch (JwtException e) {

            log.error(
                    "Error decodificando JWT: {}",
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "Error inesperado decodificando JWT: {}",
                    e.getMessage(),
                    e
            );

            throw new JwtException(
                    "Error inesperado decodificando JWT: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // AUTH0 RSA TOKEN
    // =========================================================

    private Jwt decodeAuth0Token(String token) {

        try {

            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new JwtException(
                        "Formato JWT inválido: se esperan 3 partes"
                );
            }

            String kid = extractKid(parts[0]);

            log.debug(
                    "Resolviendo clave pública RSA para kid: {}",
                    kid
            );

            RSAPublicKey publicKey =
                    resolveRsaPublicKey(kid);

            /*
             * JJWT valida:
             *
             * - firma criptográfica
             * - expiración (exp)
             * - not-before (nbf), si existe
             */
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            /*
             * Adicionalmente validamos las propiedades
             * específicas de nuestra API.
             */
            validateAuth0Issuer(claims);
            validateAuth0Audience(claims);

            log.debug(
                    "Token Auth0 validado correctamente. Subject: {}",
                    claims.getSubject()
            );

            return buildSpringJwt(
                    token,
                    claims,
                    parts[0]
            );

        } catch (JwtException e) {

            log.error(
                    "Error validando token Auth0: {}",
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "Error inesperado validando token Auth0: {}",
                    e.getMessage(),
                    e
            );

            throw new JwtException(
                    "Error validando token Auth0: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Comprueba que el issuer del token sea exactamente
     * el tenant de Auth0 configurado para esta aplicación.
     */
    private void validateAuth0Issuer(Claims claims) {

        String tokenIssuer = claims.getIssuer();

        if (tokenIssuer == null || tokenIssuer.isBlank()) {
            throw new JwtException(
                    "Token Auth0 rechazado: claim 'iss' ausente"
            );
        }

        String expectedIssuer =
                normalizeIssuer(auth0IssuerUri);

        String actualIssuer =
                normalizeIssuer(tokenIssuer);

        if (!expectedIssuer.equals(actualIssuer)) {

            log.warn(
                    "Issuer Auth0 inválido. Esperado: {}, recibido: {}",
                    expectedIssuer,
                    actualIssuer
            );

            throw new JwtException(
                    "Token Auth0 rechazado: issuer inválido"
            );
        }
    }

    /**
     * Comprueba que el token haya sido emitido para nuestra API.
     *
     * Auth0 puede representar "aud" como:
     *
     * - String
     * - Array/List de strings
     */
    private void validateAuth0Audience(Claims claims) {

        Object audienceClaim =
                claims.get("aud");

        if (audienceClaim == null) {
            throw new JwtException(
                    "Token Auth0 rechazado: claim 'aud' ausente"
            );
        }

        boolean validAudience = false;

        if (audienceClaim instanceof String audience) {

            validAudience =
                    auth0Audience.equals(audience);

        } else if (audienceClaim instanceof Collection<?> audiences) {

            validAudience = audiences.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .anyMatch(auth0Audience::equals);
        }

        if (!validAudience) {

            log.warn(
                    "Audience Auth0 inválido. Esperado: {}, recibido: {}",
                    auth0Audience,
                    audienceClaim
            );

            throw new JwtException(
                    "Token Auth0 rechazado: audience inválido"
            );
        }
    }

    private String normalizeIssuer(String issuer) {

        String normalized = issuer.trim();

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }

    // =========================================================
    // JWKS
    // =========================================================

    private RSAPublicKey resolveRsaPublicKey(String kid) {

        if (kid == null || kid.isBlank()) {
            throw new JwtException(
                    "Token Auth0 rechazado: header 'kid' ausente"
            );
        }

        try {

            JWKSet jwkSet = fetchJwkSet();

            JWK jwk =
                    jwkSet.getKeyByKeyId(kid);

            if (jwk == null) {
                throw new JwtException(
                        "No se encontró clave JWKS para kid: "
                                + kid
                );
            }

            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new JwtException(
                        "La clave JWKS encontrada no es RSA para kid: "
                                + kid
                );
            }

            return rsaKey.toRSAPublicKey();

        } catch (JwtException e) {

            throw e;

        } catch (Exception e) {

            throw new JwtException(
                    "Error resolviendo clave pública RSA: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private JWKSet fetchJwkSet() {

        long now =
                System.currentTimeMillis();

        if (
                cachedJwkSet == null
                        || (now - lastFetchTime) > CACHE_TTL_MS
        ) {

            synchronized (this) {

                if (
                        cachedJwkSet == null
                                || (now - lastFetchTime)
                                > CACHE_TTL_MS
                ) {

                    try {

                        String base =
                                normalizeIssuer(auth0IssuerUri);

                        String jwksUrl =
                                base
                                        + "/.well-known/jwks.json";

                        cachedJwkSet =
                                JWKSet.load(
                                        new URI(jwksUrl)
                                                .toURL()
                                );

                        lastFetchTime = now;

                        log.debug(
                                "JWKS cache actualizado correctamente"
                        );

                    } catch (Exception e) {

                        /*
                         * Si nunca hemos obtenido las claves,
                         * no podemos validar ningún token.
                         */
                        if (cachedJwkSet == null) {

                            throw new JwtException(
                                    "No se pudo obtener JWKS del servidor de autenticación. "
                                            + "Verifique la configuración de Auth0.",
                                    e
                            );
                        }

                        /*
                         * Si ya existe cache válida,
                         * seguimos temporalmente con ella.
                         */
                        log.warn(
                                "Error actualizando JWKS; "
                                        + "se utilizará la cache anterior: {}",
                                e.getMessage()
                        );
                    }
                }
            }
        }

        return cachedJwkSet;
    }

    // =========================================================
    // LEGACY HMAC TOKEN
    // =========================================================

    private Jwt decodeLegacyToken(String token) {

        try {

            Claims claims = Jwts.parser()
                    .verifyWith(
                            Keys.hmacShaKeyFor(
                                    legacySecretBytes
                            )
                    )
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug(
                    "Token legacy validado correctamente. Subject: {}",
                    claims.getSubject()
            );

            return buildSpringJwt(
                    token,
                    claims,
                    null
            );

        } catch (Exception e) {

            log.error(
                    "Error validando token legacy: {}",
                    e.getMessage()
            );

            throw new JwtException(
                    "Error validando token legacy: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // SPRING JWT
    // =========================================================

    private Jwt buildSpringJwt(
            String token,
            Claims claims,
            String headerBase64
    ) {

        Instant issuedAt =
                claims.getIssuedAt() != null
                        ? claims.getIssuedAt()
                                .toInstant()
                        : Instant.now();

        Instant expiresAt =
                claims.getExpiration() != null
                        ? claims.getExpiration()
                                .toInstant()
                        : Instant.now()
                                .plusSeconds(3600);

        Map<String, Object> headers =
                headerBase64 != null
                        ? parseHeader(headerBase64)
                        : Map.of(
                                "alg",
                                "HS256",
                                "typ",
                                "JWT"
                        );

        return new Jwt(
                token,
                issuedAt,
                expiresAt,
                headers,
                claims
        );
    }

    // =========================================================
    // JWT HEADER HELPERS
    // =========================================================

    private String extractAlgorithmFromHeader(
            String token
    ) {

        try {

            String[] parts =
                    token.split("\\.");

            if (parts.length != 3) {
                throw new JwtException(
                        "Formato JWT inválido"
                );
            }

            String headerJson =
                    new String(
                            Base64.getUrlDecoder()
                                    .decode(parts[0]),
                            StandardCharsets.UTF_8
                    );

            return extractJsonString(
                    headerJson,
                    "alg"
            );

        } catch (JwtException e) {

            throw e;

        } catch (Exception e) {

            throw new JwtException(
                    "No se pudo extraer algoritmo del header JWT",
                    e
            );
        }
    }

    private String extractKid(
            String headerBase64
    ) {

        try {

            String headerJson =
                    new String(
                            Base64.getUrlDecoder()
                                    .decode(headerBase64),
                            StandardCharsets.UTF_8
                    );

            return extractJsonString(
                    headerJson,
                    "kid"
            );

        } catch (Exception e) {

            throw new JwtException(
                    "No se pudo extraer kid del header JWT",
                    e
            );
        }
    }

    private Map<String, Object> parseHeader(
            String headerBase64
    ) {

        String json =
                new String(
                        Base64.getUrlDecoder()
                                .decode(headerBase64),
                        StandardCharsets.UTF_8
                );

        return Map.of(
                "alg",
                extractJsonString(
                        json,
                        "alg"
                ),
                "typ",
                extractJsonString(
                        json,
                        "typ"
                )
        );
    }

    /**
     * Extrae un valor String sencillo del JSON del header.
     *
     * Solo se utiliza para campos controlados del JWT:
     * alg, kid y typ.
     */
    private String extractJsonString(
            String json,
            String key
    ) {

        /*
         * Permitimos espacios alrededor de ":" porque el JSON
         * no necesariamente tiene que venir minificado exactamente
         * de la misma manera.
         */
        String pattern =
                "\"" + key + "\"";

        int keyStart =
                json.indexOf(pattern);

        if (keyStart == -1) {
            throw new IllegalArgumentException(
                    "Key '" + key
                            + "' no encontrada en JSON"
            );
        }

        int colon =
                json.indexOf(
                        ":",
                        keyStart
                                + pattern.length()
                );

        if (colon == -1) {
            throw new IllegalArgumentException(
                    "JSON inválido para key '"
                            + key + "'"
            );
        }

        int firstQuote =
                json.indexOf(
                        "\"",
                        colon + 1
                );

        if (firstQuote == -1) {
            throw new IllegalArgumentException(
                    "Valor inválido para key '"
                            + key + "'"
            );
        }

        int secondQuote =
                json.indexOf(
                        "\"",
                        firstQuote + 1
                );

        if (secondQuote == -1) {
            throw new IllegalArgumentException(
                    "Valor inválido para key '"
                            + key + "'"
            );
        }

        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }
}