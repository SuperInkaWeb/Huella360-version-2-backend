package com.vet_saas.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopServicioDto {
    private Long servicioId;
    private String nombreServicio;
    private Long totalCitas;
    private BigDecimal totalIngresos;
}
