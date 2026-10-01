package com.vet_saas.modules.admin.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceToggleUserTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void toggleUserStatus_desactivaAOtroUsuario() {
        Usuario cliente = Usuario.builder().id(6L).correo("cliente@test.com").rol(Role.CLIENTE).estado(true).build();
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(cliente));

        adminService.toggleUserStatus(6L, 5L);

        assertFalse(cliente.isEstado());
        verify(usuarioRepository).save(cliente);
    }

    @Test
    void toggleUserStatus_noPermiteQueElAdminSeDesactiveASiMismo() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminService.toggleUserStatus(5L, 5L));

        assertEquals("No puedes desactivar tu propia cuenta.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }
}
