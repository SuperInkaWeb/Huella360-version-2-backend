package com.vet_saas.modules.delivery.repository;

import com.vet_saas.modules.delivery.model.TrackingRepartidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingRepartidorRepository extends JpaRepository<TrackingRepartidor, Long> {

    @Query("SELECT t FROM TrackingRepartidor t WHERE t.delivery.id = :deliveryId ORDER BY t.registradoAt DESC LIMIT 1")
    Optional<TrackingRepartidor> findUltimaUbicacion(@Param("deliveryId") Long deliveryId);

    @Query("SELECT t FROM TrackingRepartidor t WHERE t.delivery.id = :deliveryId ORDER BY t.registradoAt ASC")
    List<TrackingRepartidor> findRutaCompleta(@Param("deliveryId") Long deliveryId);

    @Modifying
    @Query("DELETE FROM TrackingRepartidor t WHERE t.delivery.id = :deliveryId")
    void deleteByDeliveryId(@Param("deliveryId") Long deliveryId);
}
