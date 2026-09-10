package com.vet_saas.modules.sales.repository;

import com.vet_saas.modules.sales.model.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.vet_saas.modules.sales.model.EstadoOrden;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long>, JpaSpecificationExecutor<Orden> {

        Page<Orden> findByUsuarioClienteId(Long usuarioId, Pageable pageable);

        Page<Orden> findByEmpresaId(Long empresaId, Pageable pageable);

        @Query("SELECT o FROM Orden o JOIN FETCH o.detalles d JOIN FETCH d.producto WHERE o.id = :id")
        Optional<Orden> findByIdWithDetails(@Param("id") Long id);

        Optional<Orden> findByCodigoOrden(String codigoOrden);

        /**
         * Acquires a row-level PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
         * Serializes concurrent webhook processing for the same order.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT o FROM Orden o WHERE o.codigoOrden = :codigoOrden")
        Optional<Orden> findByCodigoOrdenForUpdate(@Param("codigoOrden") String codigoOrden);

        @Query("SELECT o FROM Orden o " +
                        "JOIN FETCH o.usuarioCliente " +
                        "JOIN FETCH o.empresa " +
                        "JOIN FETCH o.detalles d " +
                        "JOIN FETCH d.producto " +
                        "WHERE o.id = :id")
        Optional<Orden> findByIdForEmail(@Param("id") Long id);

        @Query("SELECT COALESCE(SUM(o.total), 0) " +
                        "FROM Orden o " +
                        "WHERE o.empresa.id = :empresaId " +
                        "AND o.estado = :estado " +
                        "AND o.createdAt >= :fechaInicio")
        BigDecimal sumTotalByEmpresaAndEstadoAndFechaGte(
                        @Param("empresaId") Long empresaId,
                        @Param("estado") EstadoOrden estado,
                        @Param("fechaInicio") LocalDateTime fechaInicio);

        Long countByEmpresaIdAndEstado(Long empresaId, EstadoOrden estado);

        @Query("SELECT COUNT(o) " +
                        "FROM Orden o " +
                        "WHERE o.empresa.id = :empresaId " +
                        "AND o.estado = :estado " +
                        "AND o.createdAt >= :inicioDia")
        Long countByEmpresaEstadoAndFechaGte(
                        @Param("empresaId") Long empresaId,
                        @Param("estado") EstadoOrden estado,
                        @Param("inicioDia") LocalDateTime inicioDia);

        @Query("SELECT COUNT(DISTINCT o.usuarioCliente.id) " +
                        "FROM Orden o " +
                        "WHERE o.empresa.id = :empresaId " +
                        "AND o.estado = 'PAGADO'")
        Long countDistinctClientesByEmpresa(@Param("empresaId") Long empresaId);

        @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o WHERE o.estado = :estado")
        BigDecimal sumTotalByEstado(@Param("estado") EstadoOrden estado);

        @Query(value = "SELECT DATE(created_at) AS fecha, COALESCE(SUM(total), 0) AS total_ventas, COUNT(*) AS num_ventas " +
                        "FROM ordenes " +
                        "WHERE empresa_id = :empresaId " +
                        "AND estado = 'PAGADO' " +
                        "AND created_at >= :fechaInicio " +
                        "GROUP BY DATE(created_at) " +
                        "ORDER BY DATE(created_at) ASC", nativeQuery = true)
        List<Object[]> findVentasDiariasRaw(
                        @Param("empresaId") Long empresaId,
                        @Param("fechaInicio") LocalDateTime fechaInicio);

        @Query("SELECT o FROM Orden o " +
                        "JOIN FETCH o.usuarioCliente " +
                        "WHERE o.empresa.id = :empresaId " +
                        "ORDER BY o.createdAt DESC")
        List<Orden> findRecentByEmpresa(@Param("empresaId") Long empresaId,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT o.usuarioCliente.id, SUM(o.total), COUNT(o), MAX(o.createdAt) " +
                        "FROM Orden o " +
                        "WHERE o.empresa.id = :empresaId " +
                        "AND o.estado = 'PAGADO' " +
                        "AND o.usuarioCliente IS NOT NULL " +
                        "GROUP BY o.usuarioCliente.id")
        List<Object[]> findSpendSummaryByEmpresa(@Param("empresaId") Long empresaId);

        @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o WHERE o.usuarioCliente.id = :clienteId AND o.estado = 'PAGADO'")
        BigDecimal sumTotalByUsuarioClienteId(@Param("clienteId") Long clienteId);

        @Query("SELECT COUNT(o) FROM Orden o WHERE o.usuarioCliente.id = :clienteId")
        Long countByUsuarioClienteId(@Param("clienteId") Long clienteId);

        @Query("SELECT MAX(o.createdAt) FROM Orden o WHERE o.usuarioCliente.id = :clienteId")
        LocalDateTime findMaxCreatedAtByUsuarioClienteId(@Param("clienteId") Long clienteId);

        @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o " +
                        "WHERE o.usuarioCliente.id = :clienteId AND o.empresa.id = :empresaId AND o.estado = 'PAGADO'")
        BigDecimal sumTotalByUsuarioClienteIdAndEmpresaId(
                        @Param("clienteId") Long clienteId, @Param("empresaId") Long empresaId);

        @Query("SELECT COUNT(o) FROM Orden o WHERE o.usuarioCliente.id = :clienteId AND o.empresa.id = :empresaId")
        Long countByUsuarioClienteIdAndEmpresaId(
                        @Param("clienteId") Long clienteId, @Param("empresaId") Long empresaId);

        @Query("SELECT MAX(o.createdAt) FROM Orden o " +
                        "WHERE o.usuarioCliente.id = :clienteId AND o.empresa.id = :empresaId")
        LocalDateTime findMaxCreatedAtByUsuarioClienteIdAndEmpresaId(
                        @Param("clienteId") Long clienteId, @Param("empresaId") Long empresaId);
}
