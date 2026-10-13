package com.vet_saas.modules.veterinarian.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VeterinarioRequest(
                @NotBlank(message = "El nombre es obligatorio") String nombres,

                @NotBlank(message = "El apellido es obligatorio") String apellidos,

                @NotBlank(message = "La especialidad es obligatoria") String especialidad,

                @NotBlank(message = "El número de colegiatura es obligatorio") String numeroColegiatura,

                String biografia,

                @NotNull(message = "Años de experiencia es obligatorio") Integer aniosExperiencia,

                String fotoPerfilUrl,

                String mpAccessToken,
                String mpPublicKey,

                @Size(max = 255, message = "La dirección no puede superar los 255 caracteres") String direccion,

                @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres") String ciudad,

                @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90") @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90") BigDecimal latitud,

                @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180") @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180") BigDecimal longitud) {
}