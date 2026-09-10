package com.vet_saas.modules.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken
) {}