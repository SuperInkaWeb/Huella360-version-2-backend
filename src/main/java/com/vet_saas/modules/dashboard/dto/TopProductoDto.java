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
public class TopProductoDto {
    private Long productoId;
    private String nombreProducto;
    private Long cantidadVendida;
    private BigDecimal totalVendido;
}
