package com.vet_saas.modules.auth.service;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.auth.dto.SyncAuth0Request;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.vet_saas.modules.notification.service.EmailService;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceSyncTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    void syncAuth0User_createsNewUser_whenNoExistingUser() {
        SyncAuth0Request request = new SyncAuth0Request(
                "new@test.com",
                "New User",
                "auth0|new123",
                Role.CLIENTE
        );

        Usuario result = authService.syncAuth0User(request);

        assertNotNull(result.getId());
        assertEquals("new@test.com", result.getCorreo());
        assertEquals("auth0|new123", result.getAuth0Sub());
        assertEquals(Role.CLIENTE, result.getRol());
        assertTrue(result.isEstado());
        assertTrue(result.isEmailVerificado());
    }

    @Test
    void syncAuth0User_rejectsNewUser_whenRoleIsNull() {
        SyncAuth0Request request = new SyncAuth0Request(
                "norole@test.com",
                "No Role",
                "auth0|norole",
                null
        );

        assertThrows(BusinessException.class, () -> authService.syncAuth0User(request));
        assertFalse(usuarioRepository.findByCorreo("norole@test.com").isPresent());
    }

    @Test
    void syncAuth0User_updatesRole_whenExistingUserHasCLIENTE() {
        Usuario existing = Usuario.builder()
                .correo("hasrole@test.com")
                .password("encoded")
                .rol(Role.CLIENTE)
                .auth0Sub("auth0|hasrole")
                .estado(true)
                .build();
        usuarioRepository.save(existing);

        SyncAuth0Request request = new SyncAuth0Request(
                "hasrole@test.com",
                "Has Role User",
                "auth0|hasrole",
                Role.EMPRESA
        );

        Usuario result = authService.syncAuth0User(request);

        assertEquals(Role.EMPRESA, result.getRol());
    }

    @Test
    void syncAuth0User_doesNotChangeRole_whenExistingUserHasNonCLIENTERole() {
        Usuario existing = Usuario.builder()
                .correo("hasrole2@test.com")
                .password("encoded")
                .rol(Role.EMPRESA)
                .auth0Sub("auth0|hasrole2")
                .estado(true)
                .build();
        usuarioRepository.save(existing);

        SyncAuth0Request request = new SyncAuth0Request(
                "hasrole2@test.com",
                "Has Role User",
                "auth0|hasrole2",
                Role.VETERINARIO
        );

        Usuario result = authService.syncAuth0User(request);

        assertEquals(Role.EMPRESA, result.getRol());
    }

    @Test
    void syncAuth0User_updatesExistingUser_whenFoundByAuth0Sub() {
        Usuario existing = Usuario.builder()
                .correo("old@test.com")
                .password("encoded")
                .rol(Role.CLIENTE)
                .auth0Sub("auth0|existing")
                .estado(true)
                .build();
        usuarioRepository.save(existing);

        SyncAuth0Request request = new SyncAuth0Request(
                "updated@test.com",
                "Updated",
                "auth0|existing",
                Role.EMPRESA
        );

        Usuario result = authService.syncAuth0User(request);

        assertEquals(existing.getId(), result.getId());
        assertEquals("updated@test.com", result.getCorreo());
    }

    @Test
    void syncAuth0User_linksAuth0Sub_whenFoundByEmail() {
        Usuario existing = Usuario.builder()
                .correo("link@test.com")
                .password("encoded")
                .rol(Role.VETERINARIO)
                .estado(true)
                .build();
        usuarioRepository.save(existing);

        SyncAuth0Request request = new SyncAuth0Request(
                "link@test.com",
                "Linked",
                "auth0|link123",
                Role.VETERINARIO
        );

        Usuario result = authService.syncAuth0User(request);

        assertEquals("auth0|link123", result.getAuth0Sub());
    }

    @Test
    void syncAuth0User_prefersAuth0Sub_overEmail() {
        Usuario bySub = Usuario.builder()
                .correo("bysub@test.com")
                .password("encoded")
                .rol(Role.CLIENTE)
                .auth0Sub("auth0|prefer")
                .estado(true)
                .build();
        usuarioRepository.save(bySub);

        SyncAuth0Request request = new SyncAuth0Request(
                "bysub@test.com",
                "Mixed",
                "auth0|prefer",
                Role.CLIENTE
        );

        Usuario result = authService.syncAuth0User(request);

        assertEquals(bySub.getId(), result.getId());
    }
}
