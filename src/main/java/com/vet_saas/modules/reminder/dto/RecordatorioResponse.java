package com.vet_saas.modules.reminder.dto;

import com.vet_saas.modules.reminder.model.Recordatorio;
import com.vet_saas.modules.reminder.model.RecordatorioTipo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecordatorioResponse {
    private Long id;
    private Long mascotaId;
    private String mascotaNombre;
    private RecordatorioTipo tipo;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaProgramada;
    private Boolean enviado;
    private Boolean activo;

    public static RecordatorioResponse fromEntity(Recordatorio recordatorio) {
        return RecordatorioResponse.builder()
                .id(recordatorio.getId())
                .mascotaId(recordatorio.getMascota().getId())
                .mascotaNombre(recordatorio.getMascota().getNombre())
                .tipo(recordatorio.getTipo())
                .titulo(recordatorio.getTitulo())
                .descripcion(recordatorio.getDescripcion())
                .fechaProgramada(recordatorio.getFechaProgramada())
                .enviado(recordatorio.getEnviado())
                .activo(recordatorio.getActivo())
                .build();
    }
}
