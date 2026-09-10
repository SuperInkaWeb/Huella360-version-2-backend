package com.vet_saas.modules.adoption.repository;

import com.vet_saas.modules.adoption.model.Adopcion;
import com.vet_saas.modules.adoption.model.EstadoAdopcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdopcionRepository extends JpaRepository<Adopcion, Long> {

    Page<Adopcion> findByEstadoAndActivoTrue(EstadoAdopcion estado, Pageable pageable);

    Optional<Adopcion> findByIdAndActivoTrue(Long id);

    boolean existsByMascotaIdAndEstadoInAndActivoTrue(Long mascotaId, java.util.List<EstadoAdopcion> estados);

    java.util.List<Adopcion> findByPublicadoPorIdAndActivoTrue(Long usuarioId);

    // Nuevo método dedicado para perfiles públicos de empresa
    Page<Adopcion> findByPublicadoPorIdAndEstadoAndActivoTrue(Long usuarioId, EstadoAdopcion estado, Pageable pageable);
}
