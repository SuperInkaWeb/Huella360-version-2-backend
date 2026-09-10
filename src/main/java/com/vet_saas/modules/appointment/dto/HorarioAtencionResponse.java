package com.vet_saas.modules.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioAtencionResponse(
        Long id,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer capacidad,
        Boolean activo) {
}
