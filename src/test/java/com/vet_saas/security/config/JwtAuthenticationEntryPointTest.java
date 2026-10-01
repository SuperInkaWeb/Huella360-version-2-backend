package com.vet_saas.security.config;

import com.vet_saas.security.jwt.Auth0JwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    void cuentaDesactivada_respondeAccountDisabled() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("GET", "/api/v1/users/me"), response,
                new DisabledException(Auth0JwtAuthenticationConverter.CUENTA_DESACTIVADA_MSG));

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"error\":\"ACCOUNT_DISABLED\""), body);
        assertTrue(body.contains("desactivada"), body);
    }

    @Test
    void cuentaDesactivadaComoCausa_tambienSeDetecta() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InvalidBearerTokenException("token", new DisabledException("x")));

        assertTrue(response.getContentAsString().contains("ACCOUNT_DISABLED"));
    }

    @Test
    void tokenInvalido_mantieneMensajeGenerico() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"error\":\"Unauthorized\""));
    }
}
