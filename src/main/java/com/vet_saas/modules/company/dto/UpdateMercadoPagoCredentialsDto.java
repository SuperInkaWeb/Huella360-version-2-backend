package com.vet_saas.modules.company.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMercadoPagoCredentialsDto {
    @NotBlank(message = "El Access Token de Mercado Pago es requerido")
    private String mpAccessToken;

    @NotBlank(message = "El Public Key de Mercado Pago es requerido")
    private String mpPublicKey;
}
