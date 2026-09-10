package com.vet_saas.modules.chat.repository;

import com.vet_saas.modules.chat.model.ChatEmpresaMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatEmpresaMensajeRepository extends JpaRepository<ChatEmpresaMensaje, Long> {

    List<ChatEmpresaMensaje> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    long countByChatRoomIdAndLeidoFalseAndRemitenteIdNot(Long chatRoomId, Long remitenteId);

    @Modifying
    @Query("UPDATE ChatEmpresaMensaje m SET m.leido = true " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.remitente.id <> :usuarioId AND m.leido = false")
    void marcarComoLeidos(@Param("chatRoomId") Long chatRoomId, @Param("usuarioId") Long usuarioId);
}
