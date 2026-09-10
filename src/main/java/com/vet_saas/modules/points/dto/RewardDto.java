package com.vet_saas.modules.points.dto;

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
public class RewardDto {
    private Long id;
    private Long empresaId;
    private String titulo;
    private String descripcion;
    private Integer costoPuntos;
    private String tipoDescuento;
    private BigDecimal valorDescuento;
    private Boolean aplicaACiertosProductos;
    private Boolean activo;
    private Integer totalCanjes;

    private List<Long> productosAplicablesIds;
}
