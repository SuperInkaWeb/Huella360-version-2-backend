package com.vet_saas.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private BigDecimal totalVentasMes;
    private BigDecimal ventasSemana;
    private BigDecimal ventasMesAnterior;
    private Long ordenesPendientes;
    private Long ordenesPagadasHoy;
    private Long clientesActivos;
    private Long citasHoy;
    private Long citasPendientes;
    private List<TopProductoDto> topProductos;
    private List<TopServicioDto> topServicios;
}
