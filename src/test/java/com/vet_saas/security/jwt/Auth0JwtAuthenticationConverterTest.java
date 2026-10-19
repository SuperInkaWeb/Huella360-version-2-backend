package com.vet_saas.security.jwt;

import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void convert_auth0Token_rechazaUsuarioDesactivado() {
        testUser.setEstado(false);
        Jwt jwt = buildAuth0Jwt(Map.of("https://vet-saas.com/email", "test@test.com"), "auth0|abc123");

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        // Debe ser DisabledException (no BadCredentials) para que el entry point responda ACCOUNT_DISABLED
        DisabledException ex = assertThrows(DisabledException.class, () -> converter.convert(jwt));
        assertEquals(Auth0JwtAuthenticationConverter.CUENTA_DESACTIVADA_MSG, ex.getMessage());
    }

    @Test
    void convert_legacyToken_rechazaUsuarioDesactivado() {
        testUser.setEstado(false);
        Jwt jwt = buildLegacyJwt(Map.of("role", "CLIENTE"), "42");

        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(testUser));

        assertThrows(DisabledException.class, () -> converter.convert(jwt));
    }

    // ─── H360-SEC-002: estado real del correo ───────────────────────────────
    // Antes todo usuario creado desde Auth0 quedaba con el correo verificado, aunque no lo estuviera.

    private Usuario crearDesdeToken(Map<String, Object> claims) {
        Jwt jwt = buildAuth0Jwt(claims, "auth0|nuevo");
        when(usuarioRepository.findByCorreo("nuevo@test.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByAuth0Sub("auth0|nuevo")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setEstado(true); // lo hace @PrePersist al guardar de verdad
            return u;
        });

        converter.convert(jwt);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        return guardado.getValue();
    }

    @Test
    void convert_auth0Token_usuarioNuevo_correoNoVerificado() {
        Usuario creado = crearDesdeToken(Map.of(
                "https://huella360.com/email", "nuevo@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, false));

        assertFalse(creado.isEmailVerificado());
    }

    @Test
    void convert_auth0Token_usuarioNuevo_correoVerificado() {
        Usuario creado = crearDesdeToken(Map.of(
                "https://huella360.com/email", "nuevo@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, true));

        assertTrue(creado.isEmailVerificado());
    }

    @Test
    void convert_auth0Token_usuarioNuevo_sinClaim_mantieneElComportamientoAnterior() {
        // Mientras la Action de Auth0 (H360-SEC-001) no envie el dato, no cambia nada.
        Usuario creado = crearDesdeToken(Map.of("https://huella360.com/email", "nuevo@test.com"));

        assertTrue(creado.isEmailVerificado());
    }

    @Test
    void convert_auth0Token_usuarioNuevo_aceptaElClaimEstandarComoTexto() {
        Usuario creado = crearDesdeToken(Map.of(
                "https://huella360.com/email", "nuevo@test.com",
                "email_verified", "false"));

        assertFalse(creado.isEmailVerificado());
    }

    @Test
    void convert_auth0Token_usuarioExistente_verificaElCorreoCuandoAuth0LoConfirma() {
        testUser.setAuth0Sub("auth0|abc123");
        testUser.setEmailVerificado(false);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, true), "auth0|abc123");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(testUser)).thenReturn(testUser);

        converter.convert(jwt);

        assertTrue(testUser.isEmailVerificado());
        verify(usuarioRepository).save(testUser);
    }

    @Test
    void convert_auth0Token_usuarioExistente_marcadoComoVerificadoSinEstarlo_seCorrige() {
        testUser.setAuth0Sub("auth0|abc123");
        testUser.setEmailVerificado(true);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, false), "auth0|abc123");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(testUser)).thenReturn(testUser);

        converter.convert(jwt);

        assertFalse(testUser.isEmailVerificado());
        verify(usuarioRepository).save(testUser);
    }

    @Test
    void convert_auth0Token_usuarioExistente_sinCambios_noGuarda() {
        testUser.setAuth0Sub("auth0|abc123");
        testUser.setEmailVerificado(true);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, true), "auth0|abc123");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        converter.convert(jwt);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void convert_auth0Token_usuarioExistente_sinClaim_noCambiaNiGuarda() {
        testUser.setEmailVerificado(false);
        Jwt jwt = buildAuth0Jwt(Map.of("https://huella360.com/email", "test@test.com"), "auth0|abc123");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        converter.convert(jwt);

        assertFalse(testUser.isEmailVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void convert_auth0Token_otraIdentidadConCorreoSinVerificar_noEntraALaCuentaExistente() {
        // Alguien se registra en Auth0 con el correo de otra persona y no lo verifica.
        testUser.setAuth0Sub("auth0|duenio");
        testUser.setEmailVerificado(true);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, false), "auth0|otro");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        assertThrows(CorreoNoVerificadoException.class, () -> converter.convert(jwt));

        assertTrue(testUser.isEmailVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void convert_auth0Token_cuentaSinIdentidadYCorreoSinVerificar_noEntra() {
        // Cliente invitado creado al agendar una cita: la cuenta no tiene identidad de Auth0.
        testUser.setEmailVerificado(false);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, false), "auth0|nuevo");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        assertThrows(CorreoNoVerificadoException.class, () -> converter.convert(jwt));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void convert_auth0Token_cuentaSinIdentidadYCorreoVerificado_seVinculaYSeVerifica() {
        testUser.setEmailVerificado(false);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, true), "auth0|nuevo");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(testUser)).thenReturn(testUser);

        converter.convert(jwt);

        assertEquals("auth0|nuevo", testUser.getAuth0Sub());
        assertTrue(testUser.isEmailVerificado());
        verify(usuarioRepository).save(testUser);
    }

    @Test
    void convert_auth0Token_otraIdentidadConCorreoVerificado_entraSinCambiarLaIdentidadGuardada() {
        // Mismo duenio con otra conexion (p. ej. Google y correo/contrasena).
        testUser.setAuth0Sub("auth0|duenio");
        testUser.setEmailVerificado(true);
        Jwt jwt = buildAuth0Jwt(Map.of(
                "https://huella360.com/email", "test@test.com",
                Auth0JwtAuthenticationConverter.AUTH0_EMAIL_VERIFIED_CLAIM, true), "google-oauth2|123");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertEquals(testUser, result.getPrincipal());
        assertEquals("auth0|duenio", testUser.getAuth0Sub());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void convert_auth0Token_otraIdentidadSinClaim_mantieneElComportamientoAnterior() {
        testUser.setAuth0Sub("auth0|duenio");
        Jwt jwt = buildAuth0Jwt(Map.of("https://huella360.com/email", "test@test.com"), "auth0|otro");
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(testUser));

        var result = converter.convert(jwt);

        assertEquals(testUser, result.getPrincipal());
        verify(usuarioRepository, never()).save(any());
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
