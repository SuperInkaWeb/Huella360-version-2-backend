package com.vet_saas.modules.points.repository;

import com.vet_saas.modules.points.model.ConfiguracionPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionPuntosRepository extends JpaRepository<ConfiguracionPuntos, Long> {
    Optional<ConfiguracionPuntos> findByAccion(String accion);
}
