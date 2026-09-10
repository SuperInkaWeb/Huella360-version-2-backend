package com.vet_saas.security.jwt;

import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setSecret("VGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgMTIzNDU2Nzg5");
        jwt.setExpiration(3600000L);
        props.setJwt(jwt);
        jwtService = new JwtService(props);
    }

    private Usuario buildUser(Long id, Role role) {
        return Usuario.builder()
                .id(id)
                .correo("test@test.com")
                .password("encoded")
                .rol(role)
                .estado(true)
                .build();
    }

    @Test
    void generateToken_returnsNonNull() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUserId_returnsCorrectId() {
        Usuario user = buildUser(42L, Role.CLIENTE);
        String token = jwtService.generateToken(user);
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    void extractRole_returnsCorrectRole() {
        Usuario user = buildUser(1L, Role.EMPRESA);
        String token = jwtService.generateToken(user);
        assertEquals("EMPRESA", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUser() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        Usuario user1 = buildUser(1L, Role.CLIENTE);
        Usuario user2 = buildUser(2L, Role.CLIENTE);
        String token = jwtService.generateToken(user1);
        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void extractUsername_returnsSubject() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        String token = jwtService.generateToken(user);
        assertEquals("1", jwtService.extractUsername(token));
    }

    @Test
    void expiredToken_throwsExpiredJwtException() {
        AppProperties props = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setSecret("VGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgMTIzNDU2Nzg5");
        jwt.setExpiration(-1L);
        props.setJwt(jwt);
        JwtService expiredService = new JwtService(props);

        Usuario user = buildUser(1L, Role.CLIENTE);
        String token = expiredService.generateToken(user);
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    void malformedToken_throwsMalformedJwt() {
        assertThrows(MalformedJwtException.class, () -> jwtService.extractUserId("not-a-jwt-token"));
    }
}
