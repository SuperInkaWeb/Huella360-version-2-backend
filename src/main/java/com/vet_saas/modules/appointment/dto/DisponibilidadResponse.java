package com.vet_saas.modules.appointment.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Bloques del horario de atencion para un servicio y una fecha, libres y ocupados.
 * horarioConfigurado = false solo para un veterinario independiente que aun no definio su horario:
 * en ese caso el cliente propone la hora libremente.
 */
public record DisponibilidadResponse(
        boolean horarioConfigurado,
        List<Slot> slots) {

    public record Slot(LocalTime hora, boolean disponible) {
    }
}
