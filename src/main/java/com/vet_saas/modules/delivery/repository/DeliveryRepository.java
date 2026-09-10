package com.vet_saas.modules.delivery.repository;

import com.vet_saas.modules.delivery.model.Delivery;
import com.vet_saas.modules.delivery.model.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    /**
     * Bloquea la fila del delivery hasta que finalice la transacción actual.
     * Se usa al aceptar un pedido del pool para impedir que dos repartidores
     * puedan asignarse el mismo pedido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Delivery d WHERE d.id = :deliveryId")
    Optional<Delivery> findByIdForUpdate(@Param("deliveryId") Long deliveryId);

    List<Delivery> findByEstado(DeliveryStatus estado);

    @EntityGraph(attributePaths = {"orden", "orden.empresa", "orden.usuarioCliente", "repartidor"})
    @Query("SELECT d FROM Delivery d WHERE d.orden.id = :ordenId")
    Optional<Delivery> findByOrdenId(@Param("ordenId") Long ordenId);

    List<Delivery> findByRepartidorIdRepartidorAndEstadoNotIn(Long repartidorId, List<DeliveryStatus> estadosExcluidos);

    @Query("""
    SELECT d FROM Delivery d
    JOIN d.orden o
    WHERE o.empresa.id = :empresaId
      AND d.estado NOT IN :estadosExcluidos
    ORDER BY d.createdAt DESC
    """)
    List<Delivery> findActivosByEmpresa(
            @Param("empresaId") Long empresaId,
            @Param("estadosExcluidos") List<DeliveryStatus> estadosExcluidos
    );

    List<Delivery> findByRepartidorIdRepartidorAndEstadoOrderByCreatedAtDesc(Long repartidorId, DeliveryStatus estado);

    @Query("SELECT d FROM Delivery d WHERE d.repartidor.idRepartidor = :repartidorId AND d.calificacionRepartidor IS NOT NULL ORDER BY d.entregadoAt DESC")
    List<Delivery> findCalificacionesByRepartidor(@Param("repartidorId") Long repartidorId);

    @Query("SELECT COUNT(d) > 0 FROM Delivery d WHERE d.orden.id = :ordenId")
    boolean existsByOrdenId(@Param("ordenId") Long ordenId);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.repartidor.idRepartidor = :repartidorId AND d.estado NOT IN :estados")
    int countDeliveriesActivos(
        @Param("repartidorId") Long repartidorId,
        @Param("estados") List<DeliveryStatus> estados
    );

    @Modifying
    @Query("""
        UPDATE Delivery d SET
            d.repartidor = NULL,
            d.estado = 'BUSCANDO_REPARTIDOR',
            d.intentosAsignacion = 0
        WHERE d.id = :deliveryId
          AND d.repartidor.idRepartidor = :repartidorId
          AND d.estado != 'BUSCANDO_REPARTIDOR'
          AND d.estado NOT IN ('RECOGIDO', 'EN_CAMINO', 'CERCA', 'ENTREGADO')
        """)
    int liberarSiAsignadoA(
        @Param("deliveryId") Long deliveryId,
        @Param("repartidorId") Long repartidorId
    );

    @Query("""
    SELECT d FROM Delivery d
    JOIN d.orden o
    WHERE o.empresa.id = :empresaId
      AND d.calificacionProducto IS NOT NULL
    ORDER BY d.entregadoAt DESC
    """)
    List<Delivery> findRatingsByEmpresa(@Param("empresaId") Long empresaId);

    @EntityGraph(attributePaths = {
        "orden", "orden.usuarioCliente", "orden.detalles", "orden.detalles.producto", "repartidor"
    })
    @Query("SELECT DISTINCT d FROM Delivery d WHERE d.orden.empresa.id = :empresaId ORDER BY d.createdAt DESC")
    List<Delivery> findSeguimientoByEmpresaId(@Param("empresaId") Long empresaId);
}
