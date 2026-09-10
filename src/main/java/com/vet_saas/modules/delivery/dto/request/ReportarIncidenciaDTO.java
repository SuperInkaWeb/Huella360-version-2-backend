package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportarIncidenciaDTO {

    @NotBlank(message = "El motivo es requerido")
    private String motivo;

    @NotBlank(message = "La descripción es requerida")
    private String descripcion;
}
