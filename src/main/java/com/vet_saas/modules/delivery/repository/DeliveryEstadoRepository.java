package com.vet_saas.modules.delivery.repository;

import com.vet_saas.modules.delivery.model.DeliveryEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryEstadoRepository extends JpaRepository<DeliveryEstado, Long> {

    @Query("SELECT e FROM DeliveryEstado e WHERE e.delivery.id = :deliveryId ORDER BY e.registradoAt ASC")
    List<DeliveryEstado> findByDeliveryId(@Param("deliveryId") Long deliveryId);
}
