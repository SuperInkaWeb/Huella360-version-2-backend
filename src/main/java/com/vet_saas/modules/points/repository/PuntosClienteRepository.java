package com.vet_saas.modules.points.repository;

import com.vet_saas.modules.points.model.PuntosCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PuntosClienteRepository extends JpaRepository<PuntosCliente, Long> {
}
