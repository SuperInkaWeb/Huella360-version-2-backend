package com.vet_saas.modules.veterinarian.repository;

import com.vet_saas.modules.veterinarian.model.Veterinario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Veterinario> findByUsuarioId(Long usuarioId);

    boolean existsByNumeroColegiatura(String numeroColegiatura);
}