package com.vet_saas.modules.adoption.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAdoptionDto(
        @NotNull(message = "El ID de la mascota es obligatorio") Long mascotaId,

        @NotBlank(message = "El título de la adopción es obligatorio") String titulo,

        @NotBlank(message = "La historia de la mascota es obligatoria para ayudar en su adopción") String historia,

        @NotBlank(message = "Los requisitos de adopción son obligatorios") String requisitos,

        @NotBlank(message = "La ciudad de ubicación es obligatoria") String ubicacionCiudad) {
}
