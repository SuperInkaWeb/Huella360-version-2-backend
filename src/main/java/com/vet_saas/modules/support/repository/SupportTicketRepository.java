package com.vet_saas.modules.support.repository;

import com.vet_saas.modules.support.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByPublicId(UUID publicId);

    List<SupportTicket> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
}
