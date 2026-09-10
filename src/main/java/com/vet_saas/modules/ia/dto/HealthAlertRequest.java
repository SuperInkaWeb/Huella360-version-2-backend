package com.vet_saas.modules.ia.dto;

import jakarta.validation.constraints.NotNull;

public record HealthAlertRequest(
    @NotNull Long mascotaId,
    String diagnostico,
    String tratamiento,
    String notas,
    Double pesoKg
) {}
