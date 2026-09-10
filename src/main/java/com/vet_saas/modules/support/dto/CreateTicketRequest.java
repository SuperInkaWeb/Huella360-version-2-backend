package com.vet_saas.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede superar los 255 caracteres")
    private String asunto;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private String categoria;
}
