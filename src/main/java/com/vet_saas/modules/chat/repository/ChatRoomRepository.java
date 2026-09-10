package com.vet_saas.modules.chat.repository;

import com.vet_saas.modules.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByEmpresaIdAndClienteId(Long empresaId, Long clienteId);

    Optional<ChatRoom> findByIdAndClienteId(Long id, Long clienteId);

    Optional<ChatRoom> findByIdAndEmpresaId(Long id, Long empresaId);

    List<ChatRoom> findByClienteIdOrderByUpdatedAtDesc(Long clienteId);

    List<ChatRoom> findByEmpresaIdOrderByUpdatedAtDesc(Long empresaId);
}
