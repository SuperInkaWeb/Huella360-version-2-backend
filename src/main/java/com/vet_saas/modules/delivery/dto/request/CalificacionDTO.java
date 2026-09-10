package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CalificacionDTO {

    // Short es compatible con @Min/@Max y coincide con SMALLINT en BD
    @NotNull
    @Min(1) @Max(5)
    private Short calificacionRepartidor;

    private String comentarioRepartidor;

    @NotNull
    @Min(1) @Max(5)
    private Short calificacionProducto;

    private String comentarioProducto;
}
