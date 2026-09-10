package com.vet_saas.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es requerida") String oldPassword,

        @NotBlank(message = "La nueva contraseña es requerida") @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres") String newPassword,

        @NotBlank(message = "La confirmación de la contraseña es requerida") String confirmPassword) {
}
