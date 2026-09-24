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

    boolean existsByEmpresaIdAndMascotaId(Long empresaId, Long mascotaId);

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

    // Cuenta solo citas vigentes (sin RECHAZADA/CANCELADA) y suma ingresos solo de las COMPLETADA:
    // antes una cita rechazada figuraba como servicio vendido y como ingreso.
    // Los estados van como parametros: `estado` es un enum nativo de PostgreSQL (appointment_status)
    // y un literal del enum en el JPQL se compara como varchar ("el operador no existe").
    @Query("SELECT new com.vet_saas.modules.dashboard.dto.TopServicioDto(" +
            "s.id, s.nombre, COUNT(c), " +
            "COALESCE(SUM(CASE WHEN c.estado = :completada THEN c.servicio.precio ELSE 0 END), 0)) " +
            "FROM Cita c JOIN c.servicio s " +
            "WHERE c.empresa.id = :empresaId " +
            "AND c.estado <> :rechazada AND c.estado <> :cancelada " +
            "GROUP BY s.id, s.nombre " +
            "ORDER BY COUNT(c) DESC")
    List<TopServicioDto> findTopServiciosByEmpresa(@Param("empresaId") Long empresaId,
                                                   @Param("completada") AppointmentStatus completada,
                                                   @Param("rechazada") AppointmentStatus rechazada,
                                                   @Param("cancelada") AppointmentStatus cancelada,
                                                   Pageable pageable);

    default List<TopServicioDto> findTopServiciosByEmpresa(Long empresaId, Pageable pageable) {
        return findTopServiciosByEmpresa(empresaId, AppointmentStatus.COMPLETADA,
                AppointmentStatus.RECHAZADA, AppointmentStatus.CANCELADA, pageable);
    }

    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente JOIN FETCH c.servicio " +
            "WHERE c.empresa.id = :empresaId " +
            "ORDER BY c.createdAt DESC")
    List<Cita> findRecentByEmpresa(@Param("empresaId") Long empresaId, Pageable pageable);

    // CRM: COUNT = citas reservadas (todas); gasto y ultima visita solo de citas COMPLETADA.
    // Antes se sumaba el precio de citas rechazadas/pendientes y la "ultima visita" podia ser
    // una cita rechazada o futura. (Estados como parametro: ver findTopServiciosByEmpresa.)
    @Query("SELECT c.cliente.id, COUNT(c), " +
            "COALESCE(SUM(CASE WHEN c.estado = :completada THEN c.servicio.precio ELSE 0 END), 0), " +
            "MAX(CASE WHEN c.estado = :completada THEN c.fechaProgramada ELSE NULL END) " +
            "FROM Cita c " +
            "WHERE c.empresa.id = :empresaId " +
            "GROUP BY c.cliente.id")
    List<Object[]> findCitaSummaryByEmpresa(@Param("empresaId") Long empresaId,
                                            @Param("completada") AppointmentStatus completada);

    default List<Object[]> findCitaSummaryByEmpresa(Long empresaId) {
        return findCitaSummaryByEmpresa(empresaId, AppointmentStatus.COMPLETADA);
    }

    @Query("SELECT DISTINCT c.cliente.id FROM Cita c " +
            "WHERE c.empresa.id = :empresaId AND c.cliente IS NOT NULL " +
            "AND (c.estado = :confirmada OR c.estado = :completada)")
    List<Long> findClienteIdsConCitasVigentesByEmpresa(@Param("empresaId") Long empresaId,
                                                      @Param("confirmada") AppointmentStatus confirmada,
                                                      @Param("completada") AppointmentStatus completada);

    default List<Long> findClienteIdsConCitasVigentesByEmpresa(Long empresaId) {
        return findClienteIdsConCitasVigentesByEmpresa(empresaId, AppointmentStatus.CONFIRMADA, AppointmentStatus.COMPLETADA);
    }
}
