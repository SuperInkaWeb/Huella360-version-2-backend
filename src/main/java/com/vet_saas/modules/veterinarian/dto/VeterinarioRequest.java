package com.vet_saas.modules.veterinarian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VeterinarioRequest(
                @NotBlank(message = "El nombre es obligatorio") String nombres,

                @NotBlank(message = "El apellido es obligatorio") String apellidos,

                @NotBlank(message = "La especialidad es obligatoria") String especialidad,

                @NotBlank(message = "El número de colegiatura es obligatorio") String numeroColegiatura,

                String biografia,

                @NotNull(message = "Años de experiencia es obligatorio") Integer aniosExperiencia,

                String fotoPerfilUrl,

                String mpAccessToken,
                String mpPublicKey) {
}