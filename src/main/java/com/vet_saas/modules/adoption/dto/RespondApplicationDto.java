package com.vet_saas.modules.adoption.dto;

import jakarta.validation.constraints.NotNull;

public record RespondApplicationDto(
        @NotNull(message = "Debe indicar si aprueba o rechaza la solicitud") Boolean aprobar,

        String motivoRechazo) {
}
