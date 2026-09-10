package com.vet_saas.modules.teleconsulta.repository;

import com.vet_saas.modules.teleconsulta.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    List<ChatMensaje> findByConsultaIdOrderByCreatedAtAsc(Long consultaId);
}
