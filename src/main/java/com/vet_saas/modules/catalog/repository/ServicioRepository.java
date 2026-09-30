package com.vet_saas.modules.catalog.repository;

import com.vet_saas.modules.catalog.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

        // Queries de listado interno
        Page<Servicio> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

        long countByEmpresaIdAndActivoTrue(Long empresaId);

        long countByVeterinarioIdAndActivoTrue(Long veterinarioId);

        Page<Servicio> findByVeterinarioIdAndActivoTrue(Long veterinarioId, Pageable pageable);

        // Queries de acceso individual para edición (validando propiedad)
        Optional<Servicio> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

        Optional<Servicio> findByIdAndVeterinarioIdAndActivoTrue(Long id, Long veterinarioId);

        // Queries públicos para marketplace.
        // Regla de visibilidad (A3): solo servicios de empresas o veterinarios VERIFICADOS.
        @Query("SELECT s FROM Servicio s LEFT JOIN s.empresa e LEFT JOIN s.veterinario v " +
                        "WHERE s.id = :id AND s.visible = true AND s.activo = true " +
                        "AND (e.estadoValidacion = :verificado OR v.estadoValidacion = :verificado)")
        Optional<Servicio> findPublicoPorId(@Param("id") Long id, @Param("verificado") VerificationStatus verificado);

        default Optional<Servicio> findByIdAndVisibleTrueAndActivoTrue(Long id) {
                return findPublicoPorId(id, VerificationStatus.VERIFICADO);
        }

        @Query("SELECT s FROM Servicio s " +
                        "LEFT JOIN FETCH s.empresa e " +
                        "LEFT JOIN FETCH s.veterinario v " +
                        "WHERE s.visible = true AND s.activo = true " +
                        "AND (e.estadoValidacion = :verificado OR v.estadoValidacion = :verificado) " +
                        "AND (CAST(:q AS string) IS NULL OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) "
                        +
                        "OR LOWER(s.descripcion) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))) " +
                        "AND (:empresaId IS NULL OR s.empresa.id = :empresaId) " +
                        "AND (:veterinarioId IS NULL OR s.veterinario.id = :veterinarioId)")
        Page<Servicio> findMarketplaceServicesVerificados(
                        @Param("q") String q,
                        @Param("empresaId") Long empresaId,
                        @Param("veterinarioId") Long veterinarioId,
                        @Param("verificado") VerificationStatus verificado,
                        Pageable pageable);

        default Page<Servicio> findMarketplaceServices(String q, Long empresaId, Long veterinarioId,
                        Pageable pageable) {
                return findMarketplaceServicesVerificados(q, empresaId, veterinarioId, VerificationStatus.VERIFICADO,
                                pageable);
        }

}
