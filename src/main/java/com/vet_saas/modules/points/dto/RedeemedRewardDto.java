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
public class RedeemedRewardDto {
    private Long id;
    private Long recompensaId;
    private String recompensaTitulo;
    private String tipoDescuento;
    private java.math.BigDecimal valorDescuento;
    private LocalDateTime fechaCanje;
    private Boolean utilizado;
    private LocalDateTime fechaUtilizacion;
    private Long ordenId;
}
