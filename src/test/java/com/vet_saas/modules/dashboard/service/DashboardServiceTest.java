package com.vet_saas.modules.dashboard.service;

import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.dashboard.dto.DashboardMetricsDto;
import com.vet_saas.modules.sales.repository.DetalleOrdenRepository;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * H360-QA (24/09): "Clientes activos" solo contaba clientes con ordenes pagadas; una veterinaria
 * que trabaja con citas veia 0 aunque el CRM listara clientes.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private DetalleOrdenRepository detalleOrdenRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private EmpresaRepository empresaRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(ordenRepository, detalleOrdenRepository, citaRepository, empresaRepository);
    }

    @Test
    void clientesActivos_cuentaClientesConCitasAunqueNoTenganOrdenes() {
        when(ordenRepository.findClienteIdsPagadosByEmpresa(3L)).thenReturn(List.of());
        when(citaRepository.findClienteIdsConCitasVigentesByEmpresa(3L)).thenReturn(List.of(10L));

        DashboardMetricsDto m = dashboardService.getMetrics(3L);

        assertThat(m.getClientesActivos()).isEqualTo(1L);
    }

    @Test
    void clientesActivos_noDuplicaAlClienteQueTieneOrdenesYCitas() {
        when(ordenRepository.findClienteIdsPagadosByEmpresa(3L)).thenReturn(List.of(10L, 11L));
        when(citaRepository.findClienteIdsConCitasVigentesByEmpresa(3L)).thenReturn(List.of(10L, 12L));

        DashboardMetricsDto m = dashboardService.getMetrics(3L);

        assertThat(m.getClientesActivos()).isEqualTo(3L);
    }

    @Test
    void clientesActivos_ceroSinOrdenesNiCitas() {
        when(ordenRepository.findClienteIdsPagadosByEmpresa(3L)).thenReturn(List.of());
        when(citaRepository.findClienteIdsConCitasVigentesByEmpresa(3L)).thenReturn(List.of());

        assertThat(dashboardService.getMetrics(3L).getClientesActivos()).isZero();
    }
}
