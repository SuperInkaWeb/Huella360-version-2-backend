package com.vet_saas.modules.points.repository;

import com.vet_saas.modules.points.model.Recompensa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    Page<Recompensa> findByEmpresa_Id(Long idEmpresa, Pageable pageable);
    Page<Recompensa> findByEmpresa_IdAndActivoTrue(Long idEmpresa, Pageable pageable);
}
