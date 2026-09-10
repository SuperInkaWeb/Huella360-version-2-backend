package com.vet_saas.modules.delivery.repository;

import com.vet_saas.modules.delivery.model.ZonaCobertura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZonaCoberturaRepository extends JpaRepository<ZonaCobertura, Long> {

    @Query("SELECT z FROM ZonaCobertura z WHERE z.empresa.id = :empresaId AND z.activo = true")
    List<ZonaCobertura> findActivasByEmpresa(@Param("empresaId") Long empresaId);

    @Query(value = """
        SELECT z.* FROM zonas_cobertura z
        WHERE z.empresa_id = :empresaId
          AND z.activo = TRUE
          AND (
              6371 * acos(
                  LEAST(1.0,
                      cos(radians(:lat)) * cos(radians(:empresaLat)) *
                      cos(radians(:empresaLng) - radians(:lng)) +
                      sin(radians(:lat)) * sin(radians(:empresaLat))
                  )
              )
          ) <= z.radio_km
        ORDER BY z.radio_km ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ZonaCobertura> findZonaQueCobrePunto(
            @Param("empresaId") Long empresaId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("empresaLat") double empresaLat,
            @Param("empresaLng") double empresaLng
    );
}