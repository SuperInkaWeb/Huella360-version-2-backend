package com.vet_saas.modules.catalog.dto;

import com.vet_saas.modules.catalog.model.ModalidadServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateServiceDto(
        @NotBlank(message = "El nombre del servicio es obligatorio") String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio") @PositiveOrZero(message = "El precio no puede ser negativo") BigDecimal precio,

        @Positive(message = "La duración en minutos debe ser mayor a 0") Integer duracionMinutos,

        @NotNull(message = "La modalidad del servicio es obligatoria") ModalidadServicio modalidad,

        @NotNull(message = "Especifique si el servicio es visible") Boolean visible,
        Boolean activo) {
}
