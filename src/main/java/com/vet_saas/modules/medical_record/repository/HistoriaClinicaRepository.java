package com.vet_saas.modules.medical_record.repository;

import com.vet_saas.modules.medical_record.model.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    List<HistoriaClinica> findByMascotaIdOrderByFechaRegistroDesc(Long mascotaId);

    List<HistoriaClinica> findByVeterinarioId(Long veterinarioId);
}
