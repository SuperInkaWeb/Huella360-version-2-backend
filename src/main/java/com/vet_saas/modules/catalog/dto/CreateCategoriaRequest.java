package com.vet_saas.modules.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoriaRequest(
                @NotBlank(message = "El nombre de la categoría es obligatorio") String nombre,

                Long padreId,

                String iconoUrl,

                @NotNull(message = "Debe especificar si la categoría está activa") Boolean activo,

                @NotNull(message = "El orden es obligatorio") Integer orden) {
}
