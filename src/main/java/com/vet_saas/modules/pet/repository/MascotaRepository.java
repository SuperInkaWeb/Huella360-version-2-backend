package com.vet_saas.modules.pet.repository;

import com.vet_saas.modules.pet.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByUsuarioIdAndActivoTrue(Long usuarioId);

    Optional<Mascota> findByIdAndUsuarioIdAndActivoTrue(Long id, Long usuarioId);

    long countByUsuarioIdAndActivoTrue(Long usuarioId);

    @Query("SELECT DISTINCT m FROM Mascota m INNER JOIN Cita c ON c.mascota.id = m.id WHERE c.veterinario.id = :veterinarioId AND m.activo = true")
    List<Mascota> findPacientesByVeterinario(@Param("veterinarioId") Long veterinarioId);

    @Query("SELECT DISTINCT m FROM Mascota m INNER JOIN Cita c ON c.mascota.id = m.id WHERE c.empresa.id = :empresaId AND m.activo = true")
    List<Mascota> findPacientesByEmpresa(@Param("empresaId") Long empresaId);
}
