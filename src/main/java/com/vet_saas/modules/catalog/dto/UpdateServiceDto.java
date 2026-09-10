package com.vet_saas.modules.catalog.dto;

import com.vet_saas.modules.catalog.model.ModalidadServicio;
import java.math.BigDecimal;

public record UpdateServiceDto(
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        ModalidadServicio modalidad,
        Boolean visible,
        Boolean activo) {
}
