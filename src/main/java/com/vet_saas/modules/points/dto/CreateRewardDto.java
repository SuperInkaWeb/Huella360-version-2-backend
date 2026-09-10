package com.vet_saas.modules.points.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRewardDto {

    @NotBlank(message = "El titulo es requerido")
    private String titulo;

    private String descripcion;

    @NotNull(message = "El costo en puntos es requerido")
    @Min(value = 1, message = "El costo minimo debe ser 1 punto")
    private Integer costoPuntos;

    @NotBlank(message = "El tipo de descuento es requerido")
    private String tipoDescuento; // 'PORCENTAJE', 'MONTO_FIJO'

    @NotNull(message = "El valor del descuento es requerido")
    @Min(value = 0, message = "El valor del descuento no puede ser negativo")
    private BigDecimal valorDescuento;

    private Boolean aplicaACiertosProductos;

    private List<Long> productosAplicablesIds; // if aplicaACiertosProductos is true
}
