package com.vet_saas.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiariaDto {
    private LocalDate fecha;
    private BigDecimal total;
    private Long cantidadOrdenes;
}
