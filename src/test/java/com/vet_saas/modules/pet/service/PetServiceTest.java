package com.vet_saas.modules.pet.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.pet.dto.CreatePetDto;
import com.vet_saas.modules.pet.dto.UpdatePetDto;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.model.Sexo;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock private MascotaRepository mascotaRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PlanEnforcementService planEnforcementService;
    @Mock private PointsService pointsService;
    @Mock private ClienteRepository clienteRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(mascotaRepository, empresaRepository,
                subscriptionService, planEnforcementService, pointsService, clienteRepository);
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

    private CreatePetDto buildCreateDto() {
        return new CreatePetDto("Max", "Perro", "Labrador", Sexo.MACHO,
                LocalDate.of(2023, 1, 15), new BigDecimal("12.5"),
                true, "Vacunado");
    }

    @Test
    void createPet_client_success() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        CreatePetDto dto = buildCreateDto();

        when(mascotaRepository.countByUsuarioIdAndActivoTrue(1L)).thenReturn(0L);
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(inv -> {
            Mascota m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = petService.createPet(user, dto, null);

        assertNotNull(result);
        assertEquals("Max", result.nombre());
        verify(mascotaRepository).save(any(Mascota.class));
    }

    @Test
    void createPet_empresa_checksSubscriptionLimit() {
        Usuario user = buildUser(1L, Role.EMPRESA);
        Empresa empresa = Empresa.builder().id(10L).usuarioPropietario(user).build();
        when(empresaRepository.findByUsuarioPropietarioId(1L)).thenReturn(Optional.of(empresa));
        when(mascotaRepository.countByUsuarioIdAndActivoTrue(1L)).thenReturn(5L);
        when(subscriptionService.canAddMascota(10L, 5L)).thenReturn(false);

        CreatePetDto dto = buildCreateDto();
        assertThrows(BusinessException.class, () -> petService.createPet(user, dto, null));
    }

    @Test
    void getMyPets_returnsList() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Mascota mascota = Mascota.builder()
                .id(1L).nombre("Max").especie("Perro").activo(true).usuario(user).build();
        when(mascotaRepository.findByUsuarioIdAndActivoTrue(1L)).thenReturn(List.of(mascota));

        var result = petService.getMyPets(user);

        assertEquals(1, result.size());
        assertEquals("Max", result.get(0).nombre());
    }

    @Test
    void getPetById_notFound_throws() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        when(mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> petService.getPetById(user, 99L));
    }

    @Test
    void updatePet_updatesFields() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Mascota mascota = Mascota.builder()
                .id(1L).nombre("Max").especie("Perro").activo(true).usuario(user).build();
        when(mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(1L, 1L)).thenReturn(Optional.of(mascota));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePetDto dto = new UpdatePetDto("Maximus", null, null, null, null, null, null, null);
        var result = petService.updatePet(user, 1L, dto, null);

        assertEquals("Maximus", result.nombre());
    }

    @Test
    void deletePet_setsInactive() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Mascota mascota = Mascota.builder()
                .id(1L).nombre("Max").especie("Perro").activo(true).usuario(user).build();
        when(mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(1L, 1L)).thenReturn(Optional.of(mascota));

        petService.deletePet(user, 1L);

        assertFalse(mascota.getActivo());
        verify(mascotaRepository).save(mascota);
    }
}
