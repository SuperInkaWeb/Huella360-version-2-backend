package com.vet_saas.modules.payment.repository;

import com.vet_saas.modules.payment.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByMpPaymentId(String mpPaymentId);

    Optional<Pago> findByMpPaymentId(String mpPaymentId);
}
