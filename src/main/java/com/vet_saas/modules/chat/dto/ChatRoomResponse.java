package com.vet_saas.modules.chat.dto;

import com.vet_saas.modules.chat.model.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        Long empresaId,
        String empresaNombre,
        String empresaLogoUrl,
        Long clienteId,
        String clienteNombre,
        LocalDateTime updatedAt) {

    public static ChatRoomResponse fromEntity(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getEmpresa().getId(),
                room.getEmpresa().getNombreComercial(),
                room.getEmpresa().getLogoUrl(),
                room.getCliente().getId(),
                room.getCliente().getCorreo(),
                room.getUpdatedAt());
    }
}
