package com.vet_saas.modules.ia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.ia.dto.HealthAlertRequest;
import com.vet_saas.modules.ia.dto.HealthAlertResponse;
import com.vet_saas.modules.ia.repository.IaUsageRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * H360-SEC (B7): la IA analizaba cualquier mascota con solo conocer su id y devolvia sus datos.
 * Ahora aplica la misma regla que la historia clinica.
 */
@ExtendWith(MockitoExtension.class)
class IaServiceTest {

    @Mock private MascotaRepository mascotaRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private IaUsageRepository iaUsageRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private CitaRepository citaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;

    private IaService iaService;

    private final Usuario duenio = Usuario.builder().id(10L).rol(Role.CLIENTE).build();
    private final Usuario otroCliente = Usuario.builder().id(11L).rol(Role.CLIENTE).build();
    private final Usuario usuarioVet = Usuario.builder().id(20L).rol(Role.VETERINARIO).build();
    private final Veterinario vet = Veterinario.builder().id(5L).build();
    private final Mascota mascota = Mascota.builder().id(100L).usuario(duenio).nombre("Firulais").especie("Perro").build();
    private final HealthAlertRequest request = new HealthAlertRequest(100L, null, null, null, null);

    @BeforeEach
    void setUp() {
        // Sin GROQ_API_KEY: el servicio no sale a internet y responde con las alertas de respaldo
        iaService = new IaService(mascotaRepository, new AppProperties(), new ObjectMapper(), subscriptionService,
                iaUsageRepository, restTemplate, citaRepository, veterinarioRepository);
        when(mascotaRepository.findById(100L)).thenReturn(Optional.of(mascota));
    }

    @Test
    void elDuenio_obtieneAlertasDeSuMascota() {
        HealthAlertResponse respuesta = iaService.generateHealthAlerts(duenio, request);

        assertNotNull(respuesta);
        verify(subscriptionService).enforceIaUsage(10L);
    }

    @Test
    void otroCliente_noPuedeAnalizarUnaMascotaAjena_niConsumeSuCuota() {
        assertThrows(ForbiddenException.class, () -> iaService.generateHealthAlerts(otroCliente, request));

        verify(subscriptionService, never()).enforceIaUsage(anyLong());
        verifyNoInteractions(restTemplate, iaUsageRepository);
    }

    @Test
    void veterinarioQueAtendioLaMascota_obtieneAlertas() {
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.of(vet));
        when(citaRepository.existsByVeterinarioIdAndMascotaId(5L, 100L)).thenReturn(true);

        assertNotNull(iaService.generateHealthAlerts(usuarioVet, request));
    }

    @Test
    void veterinarioSinCitasConLaMascota_recibe403() {
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.of(vet));
        when(citaRepository.existsByVeterinarioIdAndMascotaId(5L, 100L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> iaService.generateHealthAlerts(usuarioVet, request));
        verifyNoInteractions(restTemplate, iaUsageRepository);
    }

    @Test
    void veterinarioSinPerfil_recibe403() {
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> iaService.generateHealthAlerts(usuarioVet, request));
        verify(citaRepository, never()).existsByVeterinarioIdAndMascotaId(any(), any());
    }

    @Test
    void otrosRoles_noTienenAcceso() {
        Usuario empresa = Usuario.builder().id(30L).rol(Role.EMPRESA).build();

        assertThrows(ForbiddenException.class, () -> iaService.generateHealthAlerts(empresa, request));
    }
}
