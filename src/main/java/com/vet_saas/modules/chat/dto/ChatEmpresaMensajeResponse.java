package com.vet_saas.modules.chat.dto;

import com.vet_saas.modules.chat.model.ChatEmpresaMensaje;

import java.time.LocalDateTime;

public record ChatEmpresaMensajeResponse(
        Long id,
        Long chatRoomId,
        Long remitenteId,
        String remitenteNombre,
        String contenido,
        Boolean leido,
        LocalDateTime createdAt) {

    public static ChatEmpresaMensajeResponse fromEntity(ChatEmpresaMensaje mensaje) {
        return new ChatEmpresaMensajeResponse(
                mensaje.getId(),
                mensaje.getChatRoom().getId(),
                mensaje.getRemitente().getId(),
                mensaje.getRemitente().getCorreo(),
                mensaje.getContenido(),
                mensaje.getLeido(),
                mensaje.getCreatedAt());
    }
}
