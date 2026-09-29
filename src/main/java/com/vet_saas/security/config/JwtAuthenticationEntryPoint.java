package com.vet_saas.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vet_saas.security.jwt.Auth0JwtAuthenticationConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // El frontend usa error=ACCOUNT_DISABLED para avisar al usuario en vez de "sesion expirada".
        boolean cuentaDesactivada = authException instanceof DisabledException
                || authException.getCause() instanceof DisabledException;

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "success", false,
                "status", 401,
                "error", cuentaDesactivada ? "ACCOUNT_DISABLED" : "Unauthorized",
                "message", cuentaDesactivada
                        ? Auth0JwtAuthenticationConverter.CUENTA_DESACTIVADA_MSG
                        : "No autorizado: Token inválido o ausente",
                "path", request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}