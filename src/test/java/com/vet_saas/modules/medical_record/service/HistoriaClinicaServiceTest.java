package com.vet_saas.modules.medical_record.service;

import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.medical_record.repository.HistoriaClinicaRepository;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H360-SEC: regresion para el bug donde hasEmpresaAccessToPet comparaba el id del
 * usuario dueno de la empresa contra el id del usuario dueno de la mascota -- dos
 * cuentas distintas que nunca coinciden. Resultado: una empresa jamas podia ver el
 * historial clinico de ningun paciente, ni siquiera de los que si atendio (a
 * diferencia de un veterinario, que si tiene acceso via hasVetAccessToPet).
 */
@ExtendWith(MockitoExtension.class)
class HistoriaClinicaServiceTest {

    @Mock private HistoriaClinicaRepository historiaClinicaRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private EmpresaRepository empresaRepository;

    private HistoriaClinicaService service;

    @BeforeEach
    void setUp() {
        service = new HistoriaClinicaService(
                historiaClinicaRepository, mascotaRepository, veterinarioRepository,
                citaRepository, empresaRepository);
    }

    @Test
    void hasEmpresaAccessToPet_true_cuandoLaEmpresaTuvoUnaCitaConLaMascota() {
        Empresa empresa = Empresa.builder().id(5L).build();
        when(empresaRepository.findByUsuarioPropietarioId(2L)).thenReturn(Optional.of(empresa));
        when(citaRepository.existsByEmpresaIdAndMascotaId(5L, 10L)).thenReturn(true);

        assertTrue(service.hasEmpresaAccessToPet(2L, 10L));
    }

    @Test
    void hasEmpresaAccessToPet_false_cuandoNuncaTuvoUnaCitaConEsaMascota() {
        Empresa empresa = Empresa.builder().id(5L).build();
        when(empresaRepository.findByUsuarioPropietarioId(2L)).thenReturn(Optional.of(empresa));
        when(citaRepository.existsByEmpresaIdAndMascotaId(5L, 10L)).thenReturn(false);

        assertFalse(service.hasEmpresaAccessToPet(2L, 10L));
    }

    @Test
    void hasEmpresaAccessToPet_false_cuandoNoExistePerfilDeEmpresa() {
        when(empresaRepository.findByUsuarioPropietarioId(2L)).thenReturn(Optional.empty());

        assertFalse(service.hasEmpresaAccessToPet(2L, 10L));
        verify(citaRepository, never()).existsByEmpresaIdAndMascotaId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }
}
