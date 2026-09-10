package com.vet_saas.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.vet_saas.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * JWT Decoder unificado para soportar dos tipos de tokens:
 * <p>
 * 1. Tokens de Auth0 (RS256) - validados via JWKS endpoint de Auth0
 * 2. Tokens legacy del backend (HS256) - validados con HMAC secret local
 * <p>
 * Detecta el tipo de token inspeccionando el header "alg" del JWT.
 */
@Slf4j
public class Auth0JwtDecoder implements JwtDecoder {

    private final String auth0IssuerUri;
    private final byte[] legacySecretBytes;

    // Cache de JWKS para no refetchear en cada request
    private volatile JWKSet cachedJwkSet;
    private volatile long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 3_600_000; // 1 hora

    public Auth0JwtDecoder(String auth0IssuerUri, String legacySecret) {
        this.auth0IssuerUri = auth0IssuerUri;
        this.legacySecretBytes = decodeSecret(legacySecret);
    }

    /**
     * Decodifica el secreto: intenta Base64 primero, si falla usa los bytes raw.
     * Esto soporta tanto secrets Base64 como texto plano.
     */
    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret no puede estar vacio");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            // Verificar que el resultado tiene sentido (no todos ceros)
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // No es Base64 valido, usar como texto plano
        }
        // Usar bytes del string como UTF-8 (minimo 32 bytes para HMAC-SHA256)
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret debe tener al menos 32 caracteres (256 bits). Actual: " + raw.length);
        }
        return raw;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            String algorithm = extractAlgorithmFromHeader(token);
            log.debug("Decodificando JWT con algoritmo: {}", algorithm);

            return switch (algorithm) {
                case "RS256", "RS384", "RS512" -> decodeAuth0Token(token);
                case "HS256", "HS384", "HS512" -> decodeLegacyToken(token);
                default -> throw new JwtException("Algoritmo JWT no soportado: " + algorithm);
            };
        } catch (JwtException e) {
            log.error("Error decodificando JWT: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado decodificando JWT: {}", e.getMessage(), e);
            throw new JwtException("Error inesperado decodificando JWT: " + e.getMessage(), e);
        }
    }

    // ─── Auth0 RSA Token ─────────────────────────────────────────────────────

    private Jwt decodeAuth0Token(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Formato JWT invalido (se esperan 3 partes)");
            }

            String kid = extractKid(parts[0]);
            log.debug("Resolviendo clave publica RSA para kid: {}", kid);
            RSAPublicKey publicKey = resolveRsaPublicKey(kid);

            // Validar firma RSA usando JJWT con la public key
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Token Auth0 decodificado correctamente. Subject: {}", claims.getSubject());
            return buildSpringJwt(token, claims, parts[0]);

        } catch (JwtException e) {
            log.error("Error validando token Auth0: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado validando token Auth0: {}", e.getMessage(), e);
            throw new JwtException("Error validando token Auth0: " + e.getMessage(), e);
        }
    }

    private RSAPublicKey resolveRsaPublicKey(String kid) {
        try {
            JWKSet jwkSet = fetchJwkSet();

            RSAKey rsaKey = jwkSet.getKeyByKeyId(kid).toRSAKey();
            if (rsaKey == null) {
                throw new JwtException("No se encontro clave JWKS para kid: " + kid);
            }

            return rsaKey.toRSAPublicKey();
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Error resolviendo clave publica RSA: " + e.getMessage(), e);
        }
    }

    private JWKSet fetchJwkSet() {
        long now = System.currentTimeMillis();
        if (cachedJwkSet == null || (now - lastFetchTime) > CACHE_TTL_MS) {
            synchronized (this) {
                if (cachedJwkSet == null || (now - lastFetchTime) > CACHE_TTL_MS) {
                    try {
                        String base = auth0IssuerUri.endsWith("/")
                                ? auth0IssuerUri.substring(0, auth0IssuerUri.length() - 1)
                                : auth0IssuerUri;
                        String jwksUrl = base + "/.well-known/jwks.json";
                        cachedJwkSet = JWKSet.load(new URI(jwksUrl).toURL());
                        lastFetchTime = now;
                        log.debug("JWKS cache actualizado correctamente");
                    } catch (Exception e) {
                        if (cachedJwkSet == null) {
                            throw new JwtException("No se pudo obtener JWKS del servidor de autenticación. Verifique la configuración de Auth0.", e);
                        }
                        log.warn("Error refrescacheando JWKS, usando cache anterior: {}", e.getMessage());
                    }
                }
            }
        }
        return cachedJwkSet;
    }

    // ─── Legacy HMAC Token ───────────────────────────────────────────────────

    private Jwt decodeLegacyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(legacySecretBytes))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Token legacy decodificado correctamente. Subject: {}", claims.getSubject());
            return buildSpringJwt(token, claims, null);

        } catch (Exception e) {
            log.error("Error validando token legacy: {}", e.getMessage());
            throw new JwtException("Error validando token legacy: " + e.getMessage(), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Jwt buildSpringJwt(String token, Claims claims, String headerBase64) {
        Instant issuedAt = claims.getIssuedAt() != null
                ? claims.getIssuedAt().toInstant()
                : Instant.now();
        Instant expiresAt = claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : Instant.now().plusSeconds(3600);

        Map<String, Object> headers = headerBase64 != null
                ? parseHeader(headerBase64)
                : Map.of("alg", "HS256", "typ", "JWT");

        return new Jwt(
                token,
                issuedAt,
                expiresAt,
                headers,
                claims
        );
    }

    private String extractAlgorithmFromHeader(String token) {
        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            return extractJsonString(headerJson, "alg");
        } catch (Exception e) {
            throw new JwtException("No se pudo extraer algoritmo del header JWT", e);
        }
    }

    private String extractKid(String headerBase64) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(headerBase64));
            return extractJsonString(headerJson, "kid");
        } catch (Exception e) {
            throw new JwtException("No se pudo extraer kid del header JWT", e);
        }
    }

    private Map<String, Object> parseHeader(String headerBase64) {
        String json = new String(Base64.getUrlDecoder().decode(headerBase64));
        return Map.of(
                "alg", extractJsonString(json, "alg"),
                "typ", extractJsonString(json, "typ")
        );
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new IllegalArgumentException("Key '" + key + "' no encontrada en JSON");
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
