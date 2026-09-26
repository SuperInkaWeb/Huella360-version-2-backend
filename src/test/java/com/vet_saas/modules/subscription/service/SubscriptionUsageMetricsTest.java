package com.vet_saas.modules.subscription.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.subscription.dto.SubscriptionUsageDto;
import com.vet_saas.modules.subscription.model.EstadoSuscripcion;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import com.vet_saas.modules.subscription.repository.SuscripcionRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H360-QA-VET V3: GET /subscriptions/usage/me para un VETERINARIO independiente.
 * Antes el metodo lo trataba como EMPRESA, buscaba su empresa y respondia 404.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionUsageMetricsTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private EmpresaLookupService empresaLookupService;

    @InjectMocks private SubscriptionService subscriptionService;

    private final Usuario usuarioVet = Usuario.builder().id(12L).correo("vet@test.com").rol(Role.VETERINARIO).build();

    @Test
    void veterinario_usaSuSuscripcionYSusServicios_sinBuscarEmpresa() {
        Plan free = Plan.builder().id(7L).nombre("Huella Free B2B").precioMensual(BigDecimal.ZERO)
                .limiteMascotas(0).limiteProductos(4).limiteServicios(4).tipo("B2B").activo(true).build();
        when(veterinarioRepository.findByUsuarioId(12L)).thenReturn(Optional.of(Veterinario.builder().id(2L).build()));
        when(suscripcionRepository.findByVeterinarioId(2L)).thenReturn(Optional.of(
                Suscripcion.builder().id(7L).plan(free).estado(EstadoSuscripcion.ACTIVA).build()));
        when(servicioRepository.countByVeterinarioIdAndActivoTrue(2L)).thenReturn(3L);

        SubscriptionUsageDto usage = subscriptionService.getUsageMetrics(usuarioVet);

        assertEquals(3L, usage.getCurrentServices());
        assertEquals(4, usage.getMaxServices());
        assertEquals(0L, usage.getCurrentProducts());
        verify(empresaLookupService, never()).getEmpresaFromUsuario(any());
        verify(servicioRepository, never()).countByEmpresaIdAndActivoTrue(any());
    }

    @Test
    void veterinarioSinSuscripcion_devuelveMetricasEnCeroEnVezDe404() {
        when(veterinarioRepository.findByUsuarioId(12L)).thenReturn(Optional.of(Veterinario.builder().id(2L).build()));
        when(suscripcionRepository.findByVeterinarioId(2L)).thenReturn(Optional.empty());
        when(servicioRepository.countByVeterinarioIdAndActivoTrue(2L)).thenReturn(1L);

        SubscriptionUsageDto usage = subscriptionService.getUsageMetrics(usuarioVet);

        assertEquals(1L, usage.getCurrentServices());
        assertEquals(0, usage.getMaxServices());
    }

    @Test
    void usuarioVeterinarioSinPerfil_lanzaNotFound() {
        when(veterinarioRepository.findByUsuarioId(12L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.getUsageMetrics(usuarioVet));
    }
}
