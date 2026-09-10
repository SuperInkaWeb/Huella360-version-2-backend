package com.vet_saas.modules.teleconsulta.repository;

import com.vet_saas.modules.teleconsulta.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByClienteIdOrderByCreatedAtDesc(Long clienteId);

    List<Consulta> findByVeterinarioIdOrderByCreatedAtDesc(Long veterinarioId);

    List<Consulta> findByClienteIdAndEstadoIn(Long clienteId, List<com.vet_saas.modules.teleconsulta.model.ConsultaEstado> estados);

    List<Consulta> findByVeterinarioIdAndEstadoIn(Long veterinarioId, List<com.vet_saas.modules.teleconsulta.model.ConsultaEstado> estados);

    Optional<Consulta> findByIdAndClienteId(Long id, Long clienteId);

    Optional<Consulta> findByIdAndVeterinarioId(Long id, Long veterinarioId);
}
