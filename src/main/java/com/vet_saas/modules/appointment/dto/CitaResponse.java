package com.vet_saas.modules.appointment.dto;

import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.model.Cita;
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
public class CitaResponse {
    private Long id;
    private String clienteNombre;
    private String mascotaNombre;
    private String servicioNombre;
    private String veterinarioNombre;
    private LocalDate fechaProgramada;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private AppointmentStatus estado;
    private String notasCliente;
    private String notasInternas;
    private String guestNombre;
    private String guestEmail;
    private String guestTelefono;

    public static CitaResponse fromEntity(Cita cita) {
        boolean isGuest = cita.getGuestEmail() != null;
        String nombreCliente;
        if (isGuest) {
            nombreCliente = cita.getGuestNombre() != null ? cita.getGuestNombre() : cita.getGuestEmail();
        } else {
            nombreCliente = cita.getCliente().getCorreo();
        }

        return CitaResponse.builder()
                .id(cita.getId())
                .clienteNombre(nombreCliente)
                .mascotaNombre(cita.getMascota() != null ? cita.getMascota().getNombre() : "N/A")
                .servicioNombre(cita.getServicio().getNombre())
                .veterinarioNombre(cita.getVeterinario() != null
                        ? cita.getVeterinario().getNombres() + " " + cita.getVeterinario().getApellidos()
                        : "Pendiente")
                .fechaProgramada(cita.getFechaProgramada())
                .horaInicio(cita.getHoraInicio())
                .horaFin(cita.getHoraFin())
                .estado(cita.getEstado())
                .notasCliente(cita.getNotasCliente())
                .notasInternas(cita.getNotasInternas())
                .guestNombre(cita.getGuestNombre())
                .guestEmail(cita.getGuestEmail())
                .guestTelefono(cita.getGuestTelefono())
                .build();
    }
}
