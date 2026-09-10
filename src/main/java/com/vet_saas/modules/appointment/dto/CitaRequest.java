package com.vet_saas.modules.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {
    private Long mascotaId;

    @NotNull(message = "El servicio es requerido")
    private Long servicioId;

    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    private Long veterinarioId;

    @NotNull(message = "La fecha programada es requerida")
    @Future(message = "La fecha programada debe ser en el futuro")
    private LocalDate fechaProgramada;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    private String notasCliente;
}
