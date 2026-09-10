package com.vet_saas.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private Long totalUsuarios;
    private Long totalEmpresas;
    private Long totalVeterinarios;
    private Long totalAdopciones;
    private Long totalServicios;
    private Long totalProductos;
    private Long totalOrdenes;
    private BigDecimal ingresosGlobales;
}
