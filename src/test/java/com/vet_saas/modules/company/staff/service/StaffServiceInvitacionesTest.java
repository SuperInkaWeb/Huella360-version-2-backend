package com.vet_saas.modules.company.staff.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.company.staff.model.StaffStatus;
import com.vet_saas.modules.company.staff.model.StaffVeterinario;
import com.vet_saas.modules.company.staff.repository.StaffRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H360-QA-VET V1: respuesta del veterinario a las invitaciones de una empresa.
 */
@ExtendWith(MockitoExtension.class)
class StaffServiceInvitacionesTest {

    @Mock private StaffRepository staffRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private StaffService staffService;

    private Usuario usuarioVet;
    private Veterinario vet;

    @BeforeEach
    void setUp() {
        usuarioVet = Usuario.builder().id(12L).correo("vet@test.com").rol(Role.VETERINARIO).build();
        vet = Veterinario.builder().id(2L).build();
        when(veterinarioRepository.findByUsuarioId(12L)).thenReturn(Optional.of(vet));
    }

    private StaffVeterinario invitacion(Veterinario destinatario, StaffStatus estado) {
        return StaffVeterinario.builder().id(40L).veterinario(destinatario).estado(estado).rolInterno("Consultor").build();
    }

    @Test
    void aceptarInvitacionPendiente_laActiva() {
        StaffVeterinario inv = invitacion(vet, StaffStatus.PENDIENTE);
        when(staffRepository.findById(40L)).thenReturn(Optional.of(inv));

        staffService.respondToInvitation(usuarioVet, 40L, true);

        assertEquals(StaffStatus.ACTIVO, inv.getEstado());
        verify(staffRepository).save(inv);
    }

    @Test
    void rechazarInvitacionPendiente_laMarcaRechazada() {
        StaffVeterinario inv = invitacion(vet, StaffStatus.PENDIENTE);
        when(staffRepository.findById(40L)).thenReturn(Optional.of(inv));

        staffService.respondToInvitation(usuarioVet, 40L, false);

        assertEquals(StaffStatus.RECHAZADO, inv.getEstado());
        verify(staffRepository).save(inv);
    }

    @Test
    void invitacionDeOtroVeterinario_seRechaza() {
        StaffVeterinario inv = invitacion(Veterinario.builder().id(99L).build(), StaffStatus.PENDIENTE);
        when(staffRepository.findById(40L)).thenReturn(Optional.of(inv));

        assertThrows(BusinessException.class, () -> staffService.respondToInvitation(usuarioVet, 40L, true));
        assertEquals(StaffStatus.PENDIENTE, inv.getEstado());
        verify(staffRepository, never()).save(any());
    }

    @Test
    void veterinarioRetiradoDelStaff_noPuedeReaceptarSuInvitacionVieja() {
        StaffVeterinario inv = invitacion(vet, StaffStatus.FINALIZADO);
        when(staffRepository.findById(40L)).thenReturn(Optional.of(inv));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> staffService.respondToInvitation(usuarioVet, 40L, true));
        assertTrue(ex.getMessage().contains("pendiente"));
        assertEquals(StaffStatus.FINALIZADO, inv.getEstado());
        verify(staffRepository, never()).save(any());
    }

    @Test
    void invitacionYaRechazada_noSePuedeAceptarDespues() {
        StaffVeterinario inv = invitacion(vet, StaffStatus.RECHAZADO);
        when(staffRepository.findById(40L)).thenReturn(Optional.of(inv));

        assertThrows(BusinessException.class, () -> staffService.respondToInvitation(usuarioVet, 40L, true));
        verify(staffRepository, never()).save(any());
    }
}
