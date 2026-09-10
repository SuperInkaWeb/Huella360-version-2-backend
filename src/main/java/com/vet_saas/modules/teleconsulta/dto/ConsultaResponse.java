package com.vet_saas.modules.teleconsulta.dto;

import com.vet_saas.modules.teleconsulta.model.Consulta;
import com.vet_saas.modules.teleconsulta.model.ConsultaEstado;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConsultaResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long veterinarioId;
    private String veterinarioNombre;
    private Long mascotaId;
    private String mascotaNombre;
    private ConsultaEstado estado;
    private String jitsiRoomId;
    private LocalDateTime createdAt;

    public static ConsultaResponse fromEntity(Consulta consulta) {
        return ConsultaResponse.builder()
                .id(consulta.getId())
                .clienteId(consulta.getCliente().getId())
                .clienteNombre(consulta.getCliente().getCorreo())
                .veterinarioId(consulta.getVeterinario().getId())
                .veterinarioNombre(consulta.getVeterinario().getNombres() + " " + consulta.getVeterinario().getApellidos())
                .mascotaId(consulta.getMascota() != null ? consulta.getMascota().getId() : null)
                .mascotaNombre(consulta.getMascota() != null ? consulta.getMascota().getNombre() : null)
                .estado(consulta.getEstado())
                .jitsiRoomId(consulta.getJitsiRoomId())
                .createdAt(consulta.getCreatedAt())
                .build();
    }
}
