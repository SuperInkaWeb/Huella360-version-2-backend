package com.vet_saas.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El formato del correo es inválido") String correo) {
}
