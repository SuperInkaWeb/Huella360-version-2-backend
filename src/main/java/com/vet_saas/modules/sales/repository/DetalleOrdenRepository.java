package com.vet_saas.modules.sales.repository;

import com.vet_saas.modules.dashboard.dto.TopProductoDto;
import com.vet_saas.modules.sales.model.DetalleOrden;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {

    @Query("SELECT new com.vet_saas.modules.dashboard.dto.TopProductoDto(" +
            "p.id, " +
            "p.nombre, " +
            "SUM(d.cantidad), " +
            "SUM(d.subtotal)) " +
            "FROM DetalleOrden d " +
            "JOIN d.producto p " +
            "JOIN d.orden o " +
            "WHERE o.empresa.id = :empresaId " +
            "AND o.estado = 'PAGADO' " +
            "GROUP BY p.id, p.nombre " +
            "ORDER BY SUM(d.cantidad) DESC")
    List<TopProductoDto> findTopProductos(@Param("empresaId") Long empresaId, Pageable pageable);
}
