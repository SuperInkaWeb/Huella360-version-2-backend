package com.vet_saas.security.jwt;

import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Auth0JwtAuthenticationConverterTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private Auth0JwtAuthenticationConverter converter;

    private Usuario testUser;

    @BeforeEach
    void setUp() {
        converter = new Auth0JwtAuthenticationConverter(usuarioRepository);
        testUser = Usuario.builder()
                .id(1L)
                .correo("test@test.com")
                .password("encoded-password")
                .rol(Role.CLIENTE)
                .estado(true)
                .build();
    }

    @Test
    void convert_auth0Token_findsUserByEmail() {
        Jwt jwt = buildAuth0Jwt(
                Map.of(
                        "https://vet-saas.com/email", "test@test.com",
                        "https://vet-saas.com/roles", java.util.List.of("CLIENTE")
                ),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertInstanceOf(UsernamePasswordAuthenticationToken.class, result);
        assertEquals(testUser, result.getPrincipal());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
    }

    @Test
    void convert_auth0Token_findsUserByAuth0Sub_whenEmailNotFound() {
        testUser.setAuth0Sub("auth0|abc123");
        testUser.setCorreo("linked@test.com");

        Jwt jwt = buildAuth0Jwt(
                Map.of("https://vet-saas.com/email", "unknown@test.com"),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("unknown@test.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByAuth0Sub("auth0|abc123")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertInstanceOf(UsernamePasswordAuthenticationToken.class, result);
        assertEquals(testUser, result.getPrincipal());
    }

    @Test
    void convert_auth0Token_throws_whenUserNotFound() {
        Jwt jwt = buildAuth0Jwt(
                Map.of("https://vet-saas.com/email", "nobody@test.com"),
                "auth0|xyz"
        );

        when(usuarioRepository.findByCorreo("nobody@test.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByAuth0Sub("auth0|xyz")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_auth0Token_extractsRoleFromClaim() {
        testUser.setRol(Role.EMPRESA);

        Jwt jwt = buildAuth0Jwt(
                Map.of(
                        "https://vet-saas.com/email", "test@test.com",
                        "https://vet-saas.com/roles", java.util.List.of("EMPRESA")
                ),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA")));
    }

    @Test
    void convert_auth0Token_extractsRoleFromStandardClaim() {
        testUser.setRol(Role.VETERINARIO);

        Jwt jwt = buildAuth0Jwt(
                Map.of(
                        "email", "test@test.com",
                        "role", "VETERINARIO"
                ),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VETERINARIO")));
    }

    @Test
    void convert_auth0Token_defaultsToClientRole_whenNoRoleInClaims() {
        Jwt jwt = buildAuth0Jwt(
                Map.of("https://vet-saas.com/email", "test@test.com"),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
    }

    @Test
    void convert_auth0Token_usesNickname_whenContainsAt() {
        testUser.setCorreo("nickname@test.com");

        Jwt jwt = buildAuth0Jwt(
                Map.of("nickname", "nickname@test.com"),
                "auth0|abc123"
        );

        when(usuarioRepository.findByCorreo("nickname@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertEquals(testUser, result.getPrincipal());
    }

    @Test
    void convert_auth0Token_ignoresSubAsEmail() {
        Jwt jwt = buildAuth0Jwt(
                Map.of("sub", "auth0|abc123"),
                "auth0|abc123"
        );

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_legacyToken_findsUserById() {
        testUser.setRol(Role.EMPRESA);

        Jwt jwt = buildLegacyJwt(
                Map.of("role", "EMPRESA"),
                "42"
        );

        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertInstanceOf(UsernamePasswordAuthenticationToken.class, result);
        assertEquals(testUser, result.getPrincipal());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA")));
    }

    @Test
    void convert_legacyToken_throws_whenUserNotFound() {
        Jwt jwt = buildLegacyJwt(
                Map.of("role", "CLIENTE"),
                "999"
        );

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_legacyToken_throws_whenSubjectNotNumeric() {
        Jwt jwt = buildLegacyJwt(
                Map.of("role", "CLIENTE"),
                "not-a-number"
        );

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_legacyToken_fallsBackToDbRole_whenClaimInvalid() {
        testUser.setRol(Role.VETERINARIO);

        Jwt jwt = buildLegacyJwt(
                Map.of("role", "INVALID_ROLE"),
                "42"
        );

        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VETERINARIO")));
    }

    private Jwt buildAuth0Jwt(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        return new Jwt(
                "test-token",
                now,
                now.plusSeconds(3600),
                Map.of("alg", "RS256", "typ", "JWT"),
                new java.util.HashMap<>(claims) {{
                    put("sub", subject);
                }}
        );
    }

    private Jwt buildLegacyJwt(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        return new Jwt(
                "test-token",
                now,
                now.plusSeconds(3600),
                Map.of("alg", "HS256", "typ", "JWT"),
                new java.util.HashMap<>(claims) {{
                    put("sub", subject);
                }}
        );
    }
}
