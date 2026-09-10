package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarOTPDTO {
    @NotBlank
    @Size(min = 4, max = 6)
    private String codigo;
}
