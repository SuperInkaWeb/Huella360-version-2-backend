package com.vet_saas.modules.client.repository;

import com.vet_saas.modules.client.model.PerfilCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<PerfilCliente, Long> {

        Optional<PerfilCliente> findByUsuarioId(Long usuarioId);

        Optional<PerfilCliente> findByUsuarioIdAndActivoTrue(Long usuarioId);

        boolean existsByUsuarioId(Long usuarioId);

        boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);

        @Query("SELECT c FROM PerfilCliente c WHERE c.activo = true AND (" +
                        "LOWER(c.nombres) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR " +
                        "LOWER(c.apellidos) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))")
        Page<PerfilCliente> searchByNombre(@Param("q") String q, Pageable pageable);

        @Query(value = "SELECT DISTINCT c FROM PerfilCliente c " +
                        "JOIN c.usuario u " +
                        "WHERE c.activo = true " +
                        "AND (EXISTS (SELECT 1 FROM Orden o WHERE o.usuarioCliente.id = u.id AND o.empresa.id = :empresaId) " +
                        "     OR EXISTS (SELECT 1 FROM Cita ci WHERE ci.cliente.id = u.id AND ci.empresa.id = :empresaId)) " +
                        "AND (CAST(:q AS text) IS NULL OR LOWER(c.nombres) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) " +
                        "     OR LOWER(c.apellidos) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))",
                        countQuery = "SELECT COUNT(DISTINCT c) FROM PerfilCliente c " +
                        "JOIN c.usuario u " +
                        "WHERE c.activo = true " +
                        "AND (EXISTS (SELECT 1 FROM Orden o WHERE o.usuarioCliente.id = u.id AND o.empresa.id = :empresaId) " +
                        "     OR EXISTS (SELECT 1 FROM Cita ci WHERE ci.cliente.id = u.id AND ci.empresa.id = :empresaId)) " +
                        "AND (CAST(:q AS text) IS NULL OR LOWER(c.nombres) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) " +
                        "     OR LOWER(c.apellidos) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))")
        Page<PerfilCliente> findClientesByEmpresaId(
                        @Param("empresaId") Long empresaId,
                        @Param("q") String q,
                        Pageable pageable);

}
