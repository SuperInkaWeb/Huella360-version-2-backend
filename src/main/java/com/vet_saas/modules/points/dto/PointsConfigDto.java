package com.vet_saas.modules.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConfigDto {
    private Long id;
    private String accion;
    private Integer puntosOtorgados;
    private Boolean activo;
    private String descripcion;
}
