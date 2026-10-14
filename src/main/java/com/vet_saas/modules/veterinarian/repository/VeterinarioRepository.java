package com.vet_saas.modules.veterinarian.repository;

import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Veterinario> findByUsuarioId(Long usuarioId);

    boolean existsByNumeroColegiatura(String numeroColegiatura);

    List<Veterinario> findByEstadoValidacion(VerificationStatus estadoValidacion);

    // Busqueda del panel admin (nombre, apellido, colegiatura o correo).
    @Query(value = """
            SELECT v FROM Veterinario v LEFT JOIN v.usuario u
            WHERE (LOWER(v.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(v.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                OR v.numeroColegiatura LIKE CONCAT('%', :q, '%')
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(v) FROM Veterinario v LEFT JOIN v.usuario u
            WHERE (LOWER(v.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(v.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                OR v.numeroColegiatura LIKE CONCAT('%', :q, '%')
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Veterinario> buscarParaAdmin(@Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT v FROM Veterinario v LEFT JOIN v.usuario u
            WHERE v.estadoValidacion = :estado AND (LOWER(v.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(v.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                OR v.numeroColegiatura LIKE CONCAT('%', :q, '%')
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(v) FROM Veterinario v LEFT JOIN v.usuario u
            WHERE v.estadoValidacion = :estado AND (LOWER(v.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(v.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                OR v.numeroColegiatura LIKE CONCAT('%', :q, '%')
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Veterinario> buscarParaAdminPorEstado(@Param("q") String q,
            @Param("estado") VerificationStatus estado, Pageable pageable);

    // Busqueda por cercania: veterinarios con ubicacion dentro de un recuadro alrededor del punto.
    // La distancia exacta y el radio se calculan despues en CercaniaService.
    @Query("""
            SELECT v FROM Veterinario v
            WHERE v.estadoValidacion = :estado
              AND v.ubicacionLat BETWEEN :latMin AND :latMax
              AND v.ubicacionLng BETWEEN :lngMin AND :lngMax
            """)
    List<Veterinario> buscarEnRecuadro(@Param("estado") VerificationStatus estado,
            @Param("latMin") BigDecimal latMin, @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin, @Param("lngMax") BigDecimal lngMax);
}