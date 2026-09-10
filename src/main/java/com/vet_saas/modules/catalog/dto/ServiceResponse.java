package com.vet_saas.modules.catalog.dto;

import com.vet_saas.modules.catalog.model.ModalidadServicio;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
                Long id,
                String nombre,
                String descripcion,
                BigDecimal precio,
                Integer duracionMinutos,
                ModalidadServicio modalidad,
                Boolean activo,
                Boolean visible,
                Long empresaId,
                String empresaNombre,
                String empresaTipoServicio,
                Long veterinarioId,
                String veterinarioNombres,
                String veterinarioApellidos,
                String imagenUrl,
                String mpPublicKey,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
