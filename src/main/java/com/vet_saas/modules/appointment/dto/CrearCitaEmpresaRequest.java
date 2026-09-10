package com.vet_saas.modules.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CrearCitaEmpresaRequest {

    private Long clienteId;

    private String guestNombre;

    @Email(message = "El correo del invitado no es válido")
    private String guestEmail;

    private String guestTelefono;

    private Long mascotaId;

    @NotNull(message = "El servicio es requerido")
    private Long servicioId;

    private Long veterinarioId;

    @NotNull(message = "La fecha programada es requerida")
    private LocalDate fechaProgramada;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    private String notasCliente;

    private String notasInternas;
}
