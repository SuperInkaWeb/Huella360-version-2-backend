package com.vet_saas.modules.auth.service;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.modules.auth.dto.AuthResponse;
import com.vet_saas.modules.auth.dto.RegisterRequest;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceRegisterTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailService emailService;

    // Regresión H360-030: el registro público no debe poder crear cuentas ADMIN.
    @Test
    void register_rejectsAdminRole() {
        RegisterRequest request = new RegisterRequest(
                "atacante@test.com",
                "password123",
                Role.ADMIN
        );

        assertThrows(ForbiddenException.class, () -> authService.register(request));
        assertFalse(usuarioRepository.findByCorreo("atacante@test.com").isPresent());
    }

    @Test
    void register_createsUser_whenRoleIsCliente() {
        RegisterRequest request = new RegisterRequest(
                "cliente@test.com",
                "password123",
                Role.CLIENTE
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(usuarioRepository.findByCorreo("cliente@test.com").isPresent());
        assertEquals(Role.CLIENTE, usuarioRepository.findByCorreo("cliente@test.com").get().getRol());
    }
}
