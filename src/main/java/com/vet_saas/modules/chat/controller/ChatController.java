package com.vet_saas.modules.chat.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.chat.dto.ChatEmpresaMensajeResponse;
import com.vet_saas.modules.chat.dto.ChatRoomResponse;
import com.vet_saas.modules.chat.dto.SendMessageRequest;
import com.vet_saas.modules.chat.service.ChatService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/empresa/{empresaId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> abrirChatConEmpresa(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long empresaId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getOrCreateRoom(usuario, empresaId),
                "Chat abierto"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'EMPRESA')")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMisChats(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getMyRooms(usuario),
                "Chats recuperados"));
    }

    @GetMapping("/{roomId}/mensajes")
    @PreAuthorize("hasAnyRole('CLIENTE', 'EMPRESA')")
    public ResponseEntity<ApiResponse<List<ChatEmpresaMensajeResponse>>> getMensajes(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getMensajes(usuario, roomId),
                "Mensajes recuperados"));
    }

    @PostMapping("/{roomId}/mensajes")
    @PreAuthorize("hasAnyRole('CLIENTE', 'EMPRESA')")
    public ResponseEntity<ApiResponse<ChatEmpresaMensajeResponse>> enviarMensaje(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.sendMessage(usuario, roomId, request.contenido()),
                "Mensaje enviado"));
    }

    @PatchMapping("/{roomId}/leido")
    @PreAuthorize("hasAnyRole('CLIENTE', 'EMPRESA')")
    public ResponseEntity<ApiResponse<Void>> marcarComoLeido(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long roomId) {
        chatService.marcarComoLeido(usuario, roomId);
        return ResponseEntity.ok(ApiResponse.success(null, "Mensajes marcados como leídos"));
    }
}
