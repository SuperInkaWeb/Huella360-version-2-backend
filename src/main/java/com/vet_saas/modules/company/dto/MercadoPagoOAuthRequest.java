package com.vet_saas.modules.company.dto;

public record MercadoPagoOAuthRequest(
        String code,
        String redirectUri) {
}
