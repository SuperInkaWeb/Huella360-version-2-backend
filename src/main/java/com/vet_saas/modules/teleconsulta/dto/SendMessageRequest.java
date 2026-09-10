package com.vet_saas.modules.teleconsulta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
    @NotBlank String contenido,
    String tipo
) {}
