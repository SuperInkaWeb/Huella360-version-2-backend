package com.vet_saas.modules.subscription.dto;

import com.vet_saas.modules.subscription.model.EstadoSuscripcion;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuscripcionResponseDto {
    private Long id;
    private Long empresaId;
    private Long veterinarioId;
    private PlanResponseDto plan;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoSuscripcion estado;
    private LocalDateTime mpNextPaymentDate;

    public static SuscripcionResponseDto fromEntity(Suscripcion entity) {
        if (entity == null)
            return null;
        return SuscripcionResponseDto.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null)
                .veterinarioId(entity.getVeterinario() != null ? entity.getVeterinario().getId() : null)
                .plan(PlanResponseDto.fromEntity(entity.getPlan()))
                .fechaInicio(entity.getFechaInicio())
                .fechaFin(entity.getFechaFin())
                .estado(entity.getEstado())
                .mpNextPaymentDate(entity.getMpNextPaymentDate())
                .build();
    }
}
