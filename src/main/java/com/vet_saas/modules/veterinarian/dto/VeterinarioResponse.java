package com.vet_saas.modules.veterinarian.dto;

import com.vet_saas.modules.veterinarian.model.VerificationStatus;

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
                String mpPublicKey) {
}
