package com.vet_saas.modules.teleconsulta.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConsultaRequest(
    @NotNull Long veterinarioId,
    Long mascotaId,
    String mensajeInicial
) {}
