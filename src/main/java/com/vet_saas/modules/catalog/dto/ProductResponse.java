package com.vet_saas.modules.catalog.dto;

import com.vet_saas.modules.catalog.model.EstadoProducto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
                Long id,
                String nombre,
                String descripcion,
                BigDecimal precio,
                BigDecimal precioOferta,
                BigDecimal precioActual,
                LocalDateTime ofertaInicio,
                LocalDateTime ofertaFin,
                Integer stock,
                String sku,
                EstadoProducto estado,
                List<String> imagenes,
                Long categoriaId,
                String categoriaNombre,
                Long empresaId,
                String empresaNombre,
                String empresaTipoServicio,
                String mpPublicKey,
                Boolean activo,
                Boolean visible,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}