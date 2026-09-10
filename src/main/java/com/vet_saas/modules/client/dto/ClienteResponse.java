package com.vet_saas.modules.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        Long usuarioId,
        String correo,
        String nombres,
        String apellidos,
        String telefono,
        String direccion,
        String ciudad,
        String pais,
        String fotoPerfilUrl,
        BigDecimal ubicacionLat,
        BigDecimal ubicacionLng,
        LocalDateTime updatedAt) {
}
