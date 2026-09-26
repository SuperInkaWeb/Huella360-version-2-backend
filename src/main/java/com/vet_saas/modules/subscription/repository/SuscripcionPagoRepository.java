package com.vet_saas.modules.subscription.repository;

import com.vet_saas.modules.subscription.model.SuscripcionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuscripcionPagoRepository extends JpaRepository<SuscripcionPago, Long> {

    boolean existsByMpPaymentId(String mpPaymentId);
}
