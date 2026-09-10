package com.vet_saas.modules.delivery.repository;

import com.vet_saas.modules.delivery.model.Repartidor;
import com.vet_saas.modules.delivery.model.RepartidorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repartidor r WHERE r.idRepartidor = :id")
    Optional<Repartidor> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM Repartidor r WHERE r.usuario.id = :usuarioId")
    Optional<Repartidor> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(r) > 0 FROM Repartidor r WHERE r.usuario.id = :usuarioId")
    boolean existsByUsuarioId(@Param("usuarioId") Long usuarioId);

    boolean existsByDni(String dni);

    /**
     * Busca repartidores disponibles ordenados por distancia (Haversine).
     * LEAST(1.0, ...) evita NaN en acos() por errores de punto flotante.
     */
    @Query(value = """
        SELECT r.* FROM repartidores r
        WHERE r.estado_actual = 'DISPONIBLE'
          AND r.activo = TRUE
          AND r.ubicacion_lat IS NOT NULL
          AND r.ubicacion_lng IS NOT NULL
          AND (
              6371 * acos(
                  LEAST(1.0,
                      cos(radians(:lat)) * cos(radians(r.ubicacion_lat)) *
                      cos(radians(r.ubicacion_lng) - radians(:lng)) +
                      sin(radians(:lat)) * sin(radians(r.ubicacion_lat))
                  )
              )
          ) <= :radioKm
        ORDER BY (
              6371 * acos(
                  LEAST(1.0,
                      cos(radians(:lat)) * cos(radians(r.ubicacion_lat)) *
                      cos(radians(r.ubicacion_lng) - radians(:lng)) +
                      sin(radians(:lat)) * sin(radians(r.ubicacion_lat))
                  )
              )
        ) ASC
        LIMIT :limite
        """, nativeQuery = true)
    List<Repartidor> findDisponiblesEnRadio(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radioKm") double radioKm,
            @Param("limite") int limite
    );

    /**
     * Actualiza solo ubicacion y estado (evita UPDATE completo del objeto)
     */
    @Modifying
    @Query("""
        UPDATE Repartidor r SET
            r.ubicacionLat = :lat,
            r.ubicacionLng = :lng,
            r.ultimaUbicacionAt = CURRENT_TIMESTAMP
        WHERE r.idRepartidor = :id
        """)
    void actualizarUbicacion(
            @Param("id") Long id,
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng
    );

    @Modifying
    @Query("UPDATE Repartidor r SET r.estadoActual = :estado WHERE r.idRepartidor = :id")
    void actualizarEstado(@Param("id") Long id, @Param("estado") RepartidorStatus estado);
}
