package com.vet_saas.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadRecienteDto {
    private Long id;
    private String tipo;
    private String descripcion;
    private String clienteNombre;
    private BigDecimal monto;
    private String estado;
    private LocalDateTime fecha;
}
