package com.vet_saas.modules.verification.repository;

import com.vet_saas.modules.verification.model.BusinessDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long> {

    List<BusinessDocument> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
}
