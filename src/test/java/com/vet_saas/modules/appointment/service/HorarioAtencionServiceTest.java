package com.vet_saas.modules.appointment.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.appointment.dto.HorarioAtencionRequest;
import com.vet_saas.modules.appointment.model.HorarioAtencion;
import com.vet_saas.modules.appointment.repository.HorarioAtencionRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Horario de atencion: el veterinario independiente define el suyo y un rango invertido no se guarda. */
@ExtendWith(MockitoExtension.class)
class HorarioAtencionServiceTest {

    @Mock private HorarioAtencionRepository horarioAtencionRepository;
    @Mock private EmpresaLookupService empresaLookupService;
    @Mock private VeterinarioRepository veterinarioRepository;

    @InjectMocks private HorarioAtencionService horarioAtencionService;

    private final Usuario usuarioVet = Usuario.builder().id(5L).rol(Role.VETERINARIO).build();
    private final Veterinario vet = Veterinario.builder().id(2L).nombres("Carlos").apellidos("Rivas").build();

    private HorarioAtencionRequest request(int desde, int hasta, int capacidad, boolean activo) {
        return HorarioAtencionRequest.builder().diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).capacidad(capacidad).activo(activo).build();
    }

    @Test
    void veterinario_guardaSuHorario_sinEmpresaYConCupoUno() {
        when(veterinarioRepository.findByUsuarioId(5L)).thenReturn(Optional.of(vet));
        when(horarioAtencionRepository.findByVeterinarioIdAndDiaSemana(2L, DayOfWeek.MONDAY)).thenReturn(Optional.empty());

        horarioAtencionService.guardarHorariosVeterinario(usuarioVet, List.of(request(9, 13, 4, true)));

        ArgumentCaptor<HorarioAtencion> captor = ArgumentCaptor.forClass(HorarioAtencion.class);
        verify(horarioAtencionRepository).save(captor.capture());
        HorarioAtencion guardado = captor.getValue();
        assertSame(vet, guardado.getVeterinario());
        assertNull(guardado.getEmpresa());
        assertEquals(1, guardado.getCapacidad());
        assertEquals(LocalTime.of(13, 0), guardado.getHoraFin());
        verifyNoInteractions(empresaLookupService);
    }

    @Test
    void veterinario_rangoInvertido_noSeGuarda() {
        when(veterinarioRepository.findByUsuarioId(5L)).thenReturn(Optional.of(vet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> horarioAtencionService.guardarHorariosVeterinario(usuarioVet, List.of(request(18, 9, 1, true))));
        assertEquals("La hora de fin debe ser posterior a la hora de inicio.", ex.getMessage());
        verify(horarioAtencionRepository, never()).save(any());
    }

    @Test
    void empresa_rangoInvertido_noSeGuarda_peroUnDiaInactivoNoSeValida() {
        Usuario usuarioEmpresa = Usuario.builder().id(8L).rol(Role.EMPRESA).build();
        when(empresaLookupService.getEmpresaFromUsuario(usuarioEmpresa)).thenReturn(Empresa.builder().id(3L).build());

        assertThrows(BusinessException.class,
                () -> horarioAtencionService.guardarHorarios(usuarioEmpresa, List.of(request(10, 10, 1, true))));
        verify(horarioAtencionRepository, never()).save(any());

        when(horarioAtencionRepository.findByEmpresaIdAndDiaSemana(3L, DayOfWeek.MONDAY)).thenReturn(Optional.empty());
        horarioAtencionService.guardarHorarios(usuarioEmpresa, List.of(request(18, 9, 1, false)));
        verify(horarioAtencionRepository).save(any(HorarioAtencion.class));
    }
}
