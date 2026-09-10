package com.vet_saas.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}