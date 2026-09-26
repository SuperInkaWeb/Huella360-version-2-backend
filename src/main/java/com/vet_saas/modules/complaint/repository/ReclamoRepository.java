package com.vet_saas.modules.complaint.repository;

import com.vet_saas.modules.complaint.model.EstadoReclamo;
import com.vet_saas.modules.complaint.model.Reclamo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {

    Page<Reclamo> findAllByOrderByFechaRegistroDesc(Pageable pageable);

    Page<Reclamo> findByEstadoOrderByFechaRegistroDesc(EstadoReclamo estado, Pageable pageable);
}
