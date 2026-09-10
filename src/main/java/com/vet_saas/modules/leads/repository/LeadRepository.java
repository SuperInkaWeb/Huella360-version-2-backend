package com.vet_saas.modules.leads.repository;

import com.vet_saas.modules.leads.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndEstado(Long empresaId, com.vet_saas.modules.leads.model.LeadEstado estado);
}
