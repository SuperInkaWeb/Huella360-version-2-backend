package com.vet_saas.modules.company.staff.controller;

import com.vet_saas.modules.company.staff.service.StaffService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * H360-QA-VET V1: reglas de @PreAuthorize de StaffController, evaluadas por el proxy real de
 * seguridad por metodo de Spring (sin base de datos, corre tambien sin Docker).
 * Antes, el @PreAuthorize("hasRole('EMPRESA')") de la clase alcanzaba a los endpoints de
 * invitaciones y el veterinario invitado recibia 403.
 */
@SpringJUnitConfig(StaffControllerSecurityTest.Config.class)
class StaffControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        StaffService staffService() {
            return mock(StaffService.class);
        }

        @Bean
        StaffController staffController(StaffService staffService) {
            return new StaffController(staffService);
        }
    }

    @Autowired
    private StaffController controller;

    @Test
    @WithMockUser(roles = "VETERINARIO")
    void veterinario_puedeVerAceptarYRechazarSusInvitaciones() {
        assertDoesNotThrow(() -> controller.getMyInvitations(null));
        assertDoesNotThrow(() -> controller.acceptInvitation(null, 1L));
        assertDoesNotThrow(() -> controller.rejectInvitation(null, 1L));
    }

    @Test
    @WithMockUser(roles = "EMPRESA")
    void empresa_noRespondeInvitacionesAjenas() {
        assertThrows(AccessDeniedException.class, () -> controller.getMyInvitations(null));
        assertThrows(AccessDeniedException.class, () -> controller.acceptInvitation(null, 1L));
        assertThrows(AccessDeniedException.class, () -> controller.rejectInvitation(null, 1L));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cliente_noAccedeAInvitaciones() {
        assertThrows(AccessDeniedException.class, () -> controller.getMyInvitations(null));
        assertThrows(AccessDeniedException.class, () -> controller.acceptInvitation(null, 1L));
    }

    @Test
    @WithMockUser(roles = "EMPRESA")
    void empresa_sigueGestionandoSuStaff() {
        assertDoesNotThrow(() -> controller.getMyStaff(null));
        assertDoesNotThrow(() -> controller.inviteStaff(null, null));
        assertDoesNotThrow(() -> controller.removeStaff(null, 5L));
    }

    @Test
    @WithMockUser(roles = "VETERINARIO")
    void veterinario_noGestionaStaffDeEmpresas() {
        assertThrows(AccessDeniedException.class, () -> controller.getMyStaff(null));
        assertThrows(AccessDeniedException.class, () -> controller.inviteStaff(null, null));
        assertThrows(AccessDeniedException.class, () -> controller.removeStaff(null, 5L));
    }
}
