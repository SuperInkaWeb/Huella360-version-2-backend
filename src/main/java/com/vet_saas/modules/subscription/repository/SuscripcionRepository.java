package com.vet_saas.modules.subscription.repository;

import com.vet_saas.modules.subscription.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    Optional<Suscripcion> findByEmpresaId(Long empresaId);

    Optional<Suscripcion> findByVeterinarioId(Long veterinarioId);

    Optional<Suscripcion> findByUsuarioId(Long usuarioId);
}
