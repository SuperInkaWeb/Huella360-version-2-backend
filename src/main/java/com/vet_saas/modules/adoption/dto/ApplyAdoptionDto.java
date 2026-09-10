package com.vet_saas.modules.adoption.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyAdoptionDto(
        @NotBlank(message = "El mensaje de presentación es obligatorio para postular a una adopción")
        String mensajePresentacion
) {}
