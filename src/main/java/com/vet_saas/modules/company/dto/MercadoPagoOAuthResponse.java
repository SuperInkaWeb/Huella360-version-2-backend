package com.vet_saas.modules.company.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MercadoPagoOAuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("public_key") String publicKey,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("scope") String scope) {
}
