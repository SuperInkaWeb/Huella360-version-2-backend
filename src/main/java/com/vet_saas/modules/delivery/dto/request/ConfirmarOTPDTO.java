package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarOTPDTO {
    @NotBlank
    @Size(min = 6, max = 6, message = "El PIN debe tener 6 dígitos")
    private String codigo;
}
