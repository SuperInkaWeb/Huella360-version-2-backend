package com.vet_saas.modules.catalog.dto;

import com.vet_saas.modules.catalog.model.ModalidadServicio;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateServiceDto(
        String nombre,
        String descripcion,
        BigDecimal precio,
        @Positive(message = "La duración en minutos debe ser mayor a 0") @Max(value = 480, message = "La duración no puede superar 480 minutos (8 horas)") Integer duracionMinutos,
        ModalidadServicio modalidad,
        Boolean visible,
        Boolean activo) {
}
