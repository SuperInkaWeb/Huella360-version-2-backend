package com.vet_saas.modules.points.repository;

import com.vet_saas.modules.points.model.HistorialPuntos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialPuntosRepository extends JpaRepository<HistorialPuntos, Long> {
    Page<HistorialPuntos> findByPuntosCliente_Id(Long idPerfil, Pageable pageable);
    
    // Custom check to prevent duplicate "First Time" rewards
    boolean existsByPuntosCliente_IdAndTipoAccion(Long idPerfil, String tipoAccion);
    
    // Custom check for specific reference ID to prevent duplicate rewards for same order/pet/etc.
    boolean existsByPuntosCliente_IdAndTipoAccionAndReferenciaId(Long idPerfil, String tipoAccion, Long referenciaId);
}
