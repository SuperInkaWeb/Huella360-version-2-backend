package com.vet_saas.modules.auth.service;

import com.vet_saas.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Llamadas minimas a la Management API de Auth0. Requiere una aplicacion Machine to Machine
 * autorizada para la Management API con el permiso update:users (AUTH0_MGMT_CLIENT_ID/SECRET).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Auth0ManagementClient {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    private String tokenCache;
    private Instant tokenExpira = Instant.EPOCH;

    public boolean estaConfigurado() {
        AppProperties.Auth0 auth0 = appProperties.getAuth0();
        return StringUtils.hasText(auth0.getManagementClientId())
                && StringUtils.hasText(auth0.getManagementClientSecret())
                && StringUtils.hasText(auth0.getIssuer());
    }

    /** Pide a Auth0 que vuelva a enviar el correo de verificacion al usuario. */
    public void reenviarCorreoVerificacion(String auth0UserId) {
        String url = baseUrl() + "api/v2/jobs/verification-email";
        restTemplate.postForEntity(url, conToken(Map.of("user_id", auth0UserId)), Map.class);
        log.info("Correo de verificacion reenviado por Auth0. Usuario Auth0: {}", auth0UserId);
    }

    private HttpEntity<Map<String, String>> conToken(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenDeManagement());
        return new HttpEntity<>(body, headers);
    }

    private synchronized String tokenDeManagement() {
        if (tokenCache != null && Instant.now().isBefore(tokenExpira)) {
            return tokenCache;
        }
        AppProperties.Auth0 auth0 = appProperties.getAuth0();
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "client_id", auth0.getManagementClientId(),
                "client_secret", auth0.getManagementClientSecret(),
                "audience", baseUrl() + "api/v2/");
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = restTemplate.postForObject(baseUrl() + "oauth/token", body, Map.class);
        if (respuesta == null || respuesta.get("access_token") == null) {
            throw new IllegalStateException("Auth0 no devolvio un token de Management API");
        }
        tokenCache = (String) respuesta.get("access_token");
        long expiraEn = respuesta.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        // Margen de un minuto para no usar un token a punto de vencer.
        tokenExpira = Instant.now().plusSeconds(Math.max(0, expiraEn - 60));
        return tokenCache;
    }

    private String baseUrl() {
        String issuer = appProperties.getAuth0().getIssuer();
        return issuer.endsWith("/") ? issuer : issuer + "/";
    }
}
