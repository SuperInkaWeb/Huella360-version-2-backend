package com.vet_saas.modules.ia.repository;

import com.vet_saas.modules.ia.model.IaUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IaUsageRepository extends JpaRepository<IaUsage, Long> {

    @Query("SELECT COUNT(u) FROM IaUsage u WHERE u.usuario.id = :usuarioId AND u.fecha >= :inicioMes")
    long countByUsuarioIdSince(@Param("usuarioId") Long usuarioId, @Param("inicioMes") LocalDateTime inicioMes);
}
