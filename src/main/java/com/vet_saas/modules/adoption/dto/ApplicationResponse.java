package com.vet_saas.modules.adoption.dto;

import com.vet_saas.modules.adoption.model.EstadoSolicitud;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long adopcionId,
        Long interesadoId,
        String interesadoEmail,
        String mensajePresentacion,
        EstadoSolicitud estado,
        String motivoRechazo,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaRespuesta) {
}
