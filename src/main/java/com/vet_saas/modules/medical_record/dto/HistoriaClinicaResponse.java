package com.vet_saas.modules.medical_record.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoriaClinicaResponse(
        Long id,
        Long mascotaId,
        Long veterinarioId,
        String nombreVeterinario,
        Long citaId,
        String diagnostico,
        String tratamiento,
        String notas,
        BigDecimal pesoKg,
        LocalDateTime fechaRegistro,
        LocalDateTime createdAt) {
}
