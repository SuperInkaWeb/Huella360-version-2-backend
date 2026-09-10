package com.vet_saas.modules.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateProductDto(
                @NotBlank(message = "El nombre es obligatorio") String nombre,

                String descripcion,

                @NotNull(message = "El precio es obligatorio") @PositiveOrZero(message = "El precio no puede ser negativo") BigDecimal precio,

                @PositiveOrZero(message = "El precio de oferta no puede ser negativo") BigDecimal precioOferta,

                LocalDateTime ofertaInicio,
                LocalDateTime ofertaFin,

                @NotNull(message = "El stock es obligatorio") @Min(value = 0) Integer stock,

                String sku,

                @NotNull(message = "La categoría es obligatoria") Long categoriaId,

                @NotNull(message = "Especifique si el producto es visible") Boolean visible) {
}