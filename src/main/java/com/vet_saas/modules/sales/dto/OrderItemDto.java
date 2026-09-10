package com.vet_saas.modules.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemDto(
                Long productoId,

                Long servicioId,

                @NotNull @Min(1) Integer cantidad) {
}