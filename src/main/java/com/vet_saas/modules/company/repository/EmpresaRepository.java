package com.vet_saas.modules.company.repository;

import com.vet_saas.modules.company.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByUsuarioPropietarioId(Long usuarioId);

    Optional<Empresa> findByUsuarioPropietarioId(Long usuarioId);

    org.springframework.data.domain.Page<Empresa> findByEstadoValidacion(
            com.vet_saas.modules.veterinarian.model.VerificationStatus estadoValidacion,
            org.springframework.data.domain.Pageable pageable);
}