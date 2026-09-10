package com.vet_saas.modules.adoption.repository;

import com.vet_saas.modules.adoption.model.SolicitudAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudAdopcionRepository extends JpaRepository<SolicitudAdopcion, Long> {

    List<SolicitudAdopcion> findByAdopcionId(Long adopcionId);

    boolean existsByAdopcionIdAndInteresadoId(Long adopcionId, Long interesadoId);

    List<SolicitudAdopcion> findByInteresadoIdOrderByFechaSolicitudDesc(Long interesadoId);

    @Modifying
    @Query("UPDATE SolicitudAdopcion s SET s.estado = 'RECHAZADA', s.fechaRespuesta = CURRENT_TIMESTAMP WHERE s.adopcion.id = :adopcionId AND s.id != :aprobadaId AND s.estado = 'PENDIENTE'")
    void rejectOtherApplications(@Param("adopcionId") Long adopcionId, @Param("aprobadaId") Long aprobadaId);
}
