package com.vet_saas.modules.subscription.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.referral.service.ReferralService;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * H360-EMP-001/002: límites de productos y servicios por plan de la empresa
 * (Huella Free B2B = 4 productos / 4 servicios).
 */
@ExtendWith(MockitoExtension.class)
class PlanEnforcementServiceTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private ReferralService referralService;
    @InjectMocks private PlanEnforcementService planEnforcementService;

    private void planDeEmpresa(Integer limiteProductos, Integer limiteServicios) {
        Plan plan = Plan.builder().nombre("Huella Free B2B")
                .limiteProductos(limiteProductos).limiteServicios(limiteServicios).build();
        when(subscriptionService.getSuscripcionEntityByEmpresa(7L))
                .thenReturn(Suscripcion.builder().plan(plan).build());
    }

    @Test
    void productos_permiteHastaElLimiteDelPlan() {
        planDeEmpresa(4, 4);
        assertDoesNotThrow(() -> planEnforcementService.enforceProductLimit(7L, 3));
    }

    @Test
    void productos_alLlegarAlLimite_elMensajeIndicaLimiteYPlan() {
        planDeEmpresa(4, 4);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> planEnforcementService.enforceProductLimit(7L, 4));
        assertEquals("Has alcanzado el límite de 4 producto(s) de tu plan Huella Free B2B. "
                + "Actualiza tu plan para agregar más.", ex.getMessage());
    }

    @Test
    void productos_planSinProductosIncluidos_rechazaDesdeElPrimero() {
        planDeEmpresa(0, 4);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> planEnforcementService.enforceProductLimit(7L, 0));
        assertTrue(ex.getMessage().contains("no incluye productos"));
    }

    @Test
    void productos_planIlimitado_noRechaza() {
        planDeEmpresa(-1, -1);
        assertDoesNotThrow(() -> planEnforcementService.enforceProductLimit(7L, 500));
    }

    @Test
    void servicios_alLlegarAlLimite_elMensajeIndicaLimiteYPlan() {
        planDeEmpresa(4, 4);
        assertDoesNotThrow(() -> planEnforcementService.enforceServiceLimit(7L, 3));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> planEnforcementService.enforceServiceLimit(7L, 4));
        assertEquals("Has alcanzado el límite de 4 servicio(s) de tu plan Huella Free B2B. "
                + "Actualiza tu plan para agregar más.", ex.getMessage());
    }
}
