package com.vet_saas.modules.subscription.dto;

import com.vet_saas.modules.subscription.model.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponseDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioMensual;
    private Integer limiteMascotas;
    private Integer limiteProductos;
    private Integer limiteServicios;
    private Integer limiteRecordatorios;
    private Integer limiteIaUso;
    private String tipo;
    private boolean activo;

    public static PlanResponseDto fromEntity(Plan entity) {
        if (entity == null)
            return null;
        return PlanResponseDto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .precioMensual(entity.getPrecioMensual())
                .limiteMascotas(entity.getLimiteMascotas())
                .limiteProductos(entity.getLimiteProductos())
                .limiteServicios(entity.getLimiteServicios())
                .limiteRecordatorios(entity.getLimiteRecordatorios())
                .limiteIaUso(entity.getLimiteIaUso())
                .tipo(entity.getTipo())
                .activo(entity.isActivo())
                .build();
    }
}
