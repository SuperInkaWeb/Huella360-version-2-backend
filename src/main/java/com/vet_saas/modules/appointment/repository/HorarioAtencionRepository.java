package com.vet_saas.modules.appointment.repository;

import com.vet_saas.modules.appointment.model.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    List<HorarioAtencion> findByEmpresaIdOrderByDiaSemana(Long empresaId);

    Optional<HorarioAtencion> findByEmpresaIdAndDiaSemana(Long empresaId, DayOfWeek diaSemana);
}
