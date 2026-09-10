package com.vet_saas.modules.company.staff.repository;

import com.vet_saas.modules.company.staff.model.StaffStatus;
import com.vet_saas.modules.company.staff.model.StaffVeterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<StaffVeterinario, Long> {

    boolean existsByEmpresaIdAndVeterinarioId(Long empresaId, Long veterinarioId);

    Optional<StaffVeterinario> findByEmpresaIdAndVeterinarioId(Long empresaId, Long veterinarioId);

    @Query("SELECT s FROM StaffVeterinario s " +
            "JOIN FETCH s.veterinario v " +
            "WHERE s.empresa.id = :empresaId AND s.estado = :estado")
    List<StaffVeterinario> findByEmpresaIdAndEstadoWithVeterinario(
            @Param("empresaId") Long empresaId,
            @Param("estado") StaffStatus estado
    );

    @Query("SELECT s FROM StaffVeterinario s " +
            "JOIN FETCH s.veterinario v " +
            "WHERE s.veterinario.id = :veterinarioId AND s.estado = :estado")
    List<StaffVeterinario> findByVeterinarioIdAndEstadoWithVeterinario(
            @Param("veterinarioId") Long veterinarioId,
            @Param("estado") StaffStatus estado
    );
}