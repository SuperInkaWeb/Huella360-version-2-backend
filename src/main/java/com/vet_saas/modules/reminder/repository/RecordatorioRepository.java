package com.vet_saas.modules.reminder.repository;

import com.vet_saas.modules.reminder.model.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    List<Recordatorio> findByUsuarioIdAndActivoTrueOrderByFechaProgramadaAsc(Long usuarioId);

    List<Recordatorio> findByUsuarioIdAndEnviadoFalseAndActivoTrueAndFechaProgramadaLessThanEqual(Long usuarioId, LocalDateTime fecha);

    long countByUsuarioIdAndActivoTrue(Long usuarioId);

    List<Recordatorio> findByEnviadoFalseAndActivoTrueAndFechaProgramadaLessThanEqual(LocalDateTime fecha);
}
