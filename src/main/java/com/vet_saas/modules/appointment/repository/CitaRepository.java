package com.vet_saas.modules.appointment.repository;

import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.dashboard.dto.TopServicioDto;
import com.vet_saas.modules.pet.model.Mascota;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByEmpresaId(Long empresaId);

    List<Cita> findByVeterinarioId(Long veterinarioId);

    List<Cita> findByClienteId(Long clienteId);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.cliente.id = :clienteId")
    long countByClienteId(@Param("clienteId") Long clienteId);

    List<Cita> findByEmpresaIdAndFechaProgramada(Long empresaId, LocalDate fecha);

    List<Cita> findByVeterinarioIdAndFechaProgramada(Long veterinarioId, LocalDate fecha);

    @Query("SELECT DISTINCT c.mascota FROM Cita c WHERE c.veterinario.id = :veterinarioId AND c.mascota IS NOT NULL")
    List<Mascota> findUniquePatientsByVeterinarioId(@Param("veterinarioId") Long veterinarioId);

    boolean existsByVeterinarioIdAndMascotaId(Long veterinarioId, Long mascotaId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.veterinario.id = :veterinarioId " +
            "AND c.fechaProgramada = :fecha " +
            "AND c.estado <> 'CANCELADA' " +
            "AND c.horaInicio < :horaFin AND c.horaFin > :horaInicio")
    boolean existsOverlap(@Param("veterinarioId") Long veterinarioId,
                          @Param("fecha") LocalDate fecha,
                          @Param("horaInicio") LocalTime horaInicio,
                          @Param("horaFin") LocalTime horaFin);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.empresa.id = :empresaId AND c.fechaProgramada = :fecha")
    long countByEmpresaIdAndFecha(@Param("empresaId") Long empresaId, @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.empresa.id = :empresaId AND c.estado = :estado")
    long countByEmpresaIdAndEstado(@Param("empresaId") Long empresaId, @Param("estado") AppointmentStatus estado);

    @Query("SELECT new com.vet_saas.modules.dashboard.dto.TopServicioDto(" +
            "s.id, s.nombre, COUNT(c), COALESCE(SUM(c.servicio.precio), 0)) " +
            "FROM Cita c JOIN c.servicio s " +
            "WHERE c.empresa.id = :empresaId " +
            "GROUP BY s.id, s.nombre " +
            "ORDER BY COUNT(c) DESC")
    List<TopServicioDto> findTopServiciosByEmpresa(@Param("empresaId") Long empresaId, Pageable pageable);

    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente JOIN FETCH c.servicio " +
            "WHERE c.empresa.id = :empresaId " +
            "ORDER BY c.createdAt DESC")
    List<Cita> findRecentByEmpresa(@Param("empresaId") Long empresaId, Pageable pageable);

    @Query("SELECT c.cliente.id, COUNT(c), COALESCE(SUM(c.servicio.precio), 0), MAX(c.fechaProgramada) " +
            "FROM Cita c " +
            "WHERE c.empresa.id = :empresaId " +
            "GROUP BY c.cliente.id")
    List<Object[]> findCitaSummaryByEmpresa(@Param("empresaId") Long empresaId);
}
