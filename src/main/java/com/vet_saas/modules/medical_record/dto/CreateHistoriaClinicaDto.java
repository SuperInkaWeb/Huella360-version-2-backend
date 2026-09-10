package com.vet_saas.modules.medical_record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateHistoriaClinicaDto(
        @NotNull(message = "El ID de la mascota es requerido") Long mascotaId,

        Long citaId,

        @NotBlank(message = "El diagnóstico es requerido") String diagnostico,

        @NotBlank(message = "El tratamiento es requerido") String tratamiento,

        String notas,

        @Positive(message = "El peso debe ser un valor positivo") BigDecimal pesoKg,

        LocalDateTime fechaRegistro) {
}
