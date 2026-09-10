package com.vet_saas.modules.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryDto {
    private Long id;
    private Integer puntos;
    private String tipoAccion;
    private String descripcion;
    private LocalDateTime fecha;
}
