package com.vet_saas.security.filter;

import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorreoVerificadoFilterTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void correoSinVerificar_bloqueaLasRutasPrivadas() throws Exception {
        autenticar(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new CorreoVerificadoFilter(true).doFilter(new MockHttpServletRequest("POST", "/api/v1/pets"), response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"error\":\"EMAIL_NOT_VERIFIED\""));
        assertNull(chain.getRequest(), "no debe llegar al controlador");
    }

    @Test
    void correoSinVerificar_puedeVerSuEstadoYPedirOtroEnlace() throws Exception {
        autenticar(false);

        assertPasa(new CorreoVerificadoFilter(true), "GET", "/api/v1/users/me");
        assertPasa(new CorreoVerificadoFilter(true), "POST", "/api/v1/auth/resend-verification");
        assertPasa(new CorreoVerificadoFilter(true), "GET", "/api/v1/public/products");
    }

    @Test
    void correoSinVerificar_noPuedeElegirRol() throws Exception {
        autenticar(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorreoVerificadoFilter(true).doFilter(
                new MockHttpServletRequest("PATCH", "/api/v1/users/me/role"), response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void correoVerificado_pasa() throws Exception {
        autenticar(true);

        assertPasa(new CorreoVerificadoFilter(true), "POST", "/api/v1/pets");
    }

    @Test
    void interruptorApagado_noBloqueaANadie() throws Exception {
        autenticar(false);

        assertPasa(new CorreoVerificadoFilter(false), "POST", "/api/v1/pets");
    }

    @Test
    void sinSesion_loDecideSecurityConfig() throws Exception {
        assertPasa(new CorreoVerificadoFilter(true), "POST", "/api/v1/pets");
    }

    @Test
    void rutasPermitidas() {
        assertTrue(CorreoVerificadoFilter.esPermitida("GET", "/api/v1/services/12"));
        assertTrue(CorreoVerificadoFilter.esPermitida("POST", "/api/v1/reclamos"));
        assertTrue(CorreoVerificadoFilter.esPermitida("OPTIONS", "/api/v1/pets"));
        assertFalse(CorreoVerificadoFilter.esPermitida("POST", "/api/v1/services"));
        assertFalse(CorreoVerificadoFilter.esPermitida("GET", "/api/v1/orders/me"));
    }

    private static void assertPasa(CorreoVerificadoFilter filter, String metodo, String ruta) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest(metodo, ruta), response, chain);
        assertNotNull(chain.getRequest(), metodo + " " + ruta + " deberia pasar");
        assertEquals(200, response.getStatus());
    }

    private static void autenticar(boolean emailVerificado) {
        Usuario usuario = Usuario.builder().id(1L).correo("a@test.com").rol(Role.CLIENTE)
                .estado(true).emailVerificado(emailVerificado).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
    }
}
