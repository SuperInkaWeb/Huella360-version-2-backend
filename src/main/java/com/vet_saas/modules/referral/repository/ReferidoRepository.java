package com.vet_saas.modules.referral.repository;

import com.vet_saas.modules.referral.model.Referido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferidoRepository extends JpaRepository<Referido, Long> {

    long countByUsuarioQueRefirioId(Long usuarioId);

    List<Referido> findByCodigoReferido(String codigo);

    Optional<Referido> findByUsuarioRefiridoId(Long usuarioId);

    List<Referido> findByUsuarioQueRefirioIdOrderByCreatedAtDesc(Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Referido r WHERE r.usuarioRefirido.id = :userId AND r.usuarioQueRefirio.id <> :selfId")
    boolean existsByUsuarioRefiridoIdAndNotSelfReference(@Param("userId") Long userId, @Param("selfId") Long selfId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Referido r WHERE r.usuarioRefirido.id = :userId AND r.usuarioQueRefirio.id = :referrerId")
    boolean existsByUsuarioRefiridoIdAndUsuarioQueRefirioId(@Param("userId") Long userId, @Param("referrerId") Long referrerId);
}
