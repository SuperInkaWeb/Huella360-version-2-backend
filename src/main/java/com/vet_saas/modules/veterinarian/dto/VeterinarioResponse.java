package com.vet_saas.modules.veterinarian.dto;

import com.vet_saas.modules.veterinarian.model.VerificationStatus;

import java.math.BigDecimal;

public record VeterinarioResponse(
                Long idVeterinario,
                Long idUsuario,
                String nombres,
                String apellidos,
                String especialidad,
                String numeroColegiatura,
                String biografia,
                Integer aniosExperiencia,
                String fotoPerfilUrl,
                VerificationStatus estadoValidacion,
                String email,
                String mpPublicKey,
                String direccion,
                String ciudad,
                BigDecimal latitud,
                BigDecimal longitud) {
}
