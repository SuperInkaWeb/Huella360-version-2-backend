package com.vet_saas.modules.chat.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.chat.dto.ChatEmpresaMensajeResponse;
import com.vet_saas.modules.chat.dto.ChatRoomResponse;
import com.vet_saas.modules.chat.model.ChatEmpresaMensaje;
import com.vet_saas.modules.chat.model.ChatRoom;
import com.vet_saas.modules.chat.repository.ChatEmpresaMensajeRepository;
import com.vet_saas.modules.chat.repository.ChatRoomRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatEmpresaMensajeRepository chatMensajeRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaLookupService empresaLookupService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatRoomResponse getOrCreateRoom(Usuario cliente, Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));

        ChatRoom room = chatRoomRepository.findByEmpresaIdAndClienteId(empresaId, cliente.getId())
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .empresa(empresa)
                                .cliente(cliente)
                                .build()));

        return ChatRoomResponse.fromEntity(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Usuario usuario) {
        List<ChatRoom> rooms;
        if (usuario.getRol() == Role.EMPRESA) {
            Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
            rooms = chatRoomRepository.findByEmpresaIdOrderByUpdatedAtDesc(empresa.getId());
        } else {
            rooms = chatRoomRepository.findByClienteIdOrderByUpdatedAtDesc(usuario.getId());
        }
        return rooms.stream().map(ChatRoomResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatEmpresaMensajeResponse> getMensajes(Usuario usuario, Long roomId) {
        ChatRoom room = getRoomForUser(usuario, roomId);
        return chatMensajeRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId()).stream()
                .map(ChatEmpresaMensajeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatEmpresaMensajeResponse sendMessage(Usuario usuario, Long roomId, String contenido) {
        ChatRoom room = getRoomForUser(usuario, roomId);

        ChatEmpresaMensaje mensaje = ChatEmpresaMensaje.builder()
                .chatRoom(room)
                .remitente(usuario)
                .contenido(contenido)
                .build();

        ChatEmpresaMensaje saved = chatMensajeRepository.save(mensaje);

        room.setUpdatedAt(saved.getCreatedAt());
        chatRoomRepository.save(room);

        ChatEmpresaMensajeResponse response = ChatEmpresaMensajeResponse.fromEntity(saved);
        messagingTemplate.convertAndSend("/topic/chat-empresa/" + room.getId() + "/mensajes", response);

        return response;
    }

    @Transactional
    public void marcarComoLeido(Usuario usuario, Long roomId) {
        ChatRoom room = getRoomForUser(usuario, roomId);
        chatMensajeRepository.marcarComoLeidos(room.getId(), usuario.getId());
    }

    private ChatRoom getRoomForUser(Usuario usuario, Long roomId) {
        if (usuario.getRol() == Role.EMPRESA) {
            Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
            return chatRoomRepository.findByIdAndEmpresaId(roomId, empresa.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", roomId));
        }
        return chatRoomRepository.findByIdAndClienteId(roomId, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", roomId));
    }
}
