package com.vet_saas.modules.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteCrmResponse(
        Long id,
        Long usuarioId,
        String correo,
        String nombres,
        String apellidos,
        String telefono,
        String fotoPerfilUrl,
        BigDecimal totalGastado,
        Long totalPedidos,
        Long totalCitas,
        LocalDateTime ultimaCompra) {
}