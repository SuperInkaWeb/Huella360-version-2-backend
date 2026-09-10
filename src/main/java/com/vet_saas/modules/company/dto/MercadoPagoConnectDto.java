package com.vet_saas.modules.company.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MercadoPagoConnectDto {
    @NotBlank(message = "El código es requerido")
    private String code;

    @NotBlank(message = "La URI de redirección es requerida")
    private String redirectUri;
}
