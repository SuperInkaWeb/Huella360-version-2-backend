package com.vet_saas.modules.points.repository;

import com.vet_saas.modules.points.model.CanjeRecompensa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanjeRecompensaRepository extends JpaRepository<CanjeRecompensa, Long> {
    Page<CanjeRecompensa> findByPuntosCliente_Id(Long idPerfil, Pageable pageable);
    Page<CanjeRecompensa> findByPuntosCliente_IdAndUtilizadoFalse(Long idPerfil, Pageable pageable);
    List<CanjeRecompensa> findByPuntosCliente_IdAndUtilizadoFalseAndRecompensa_Empresa_Id(Long idPerfil, Long idEmpresa);
    long countByRecompensa_Id(Long idRecompensa);
}
