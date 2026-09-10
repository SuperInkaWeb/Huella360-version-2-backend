package com.vet_saas.modules.teleconsulta.dto;

import com.vet_saas.modules.teleconsulta.model.ChatMensaje;
import com.vet_saas.modules.teleconsulta.model.MensajeTipo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMensajeResponse {
    private Long id;
    private Long remitenteId;
    private String remitenteNombre;
    private String contenido;
    private MensajeTipo tipo;
    private LocalDateTime createdAt;

    public static ChatMensajeResponse fromEntity(ChatMensaje mensaje) {
        return ChatMensajeResponse.builder()
                .id(mensaje.getId())
                .remitenteId(mensaje.getRemitente().getId())
                .remitenteNombre(mensaje.getRemitente().getCorreo())
                .contenido(mensaje.getContenido())
                .tipo(mensaje.getTipo())
                .createdAt(mensaje.getCreatedAt())
                .build();
    }
}
