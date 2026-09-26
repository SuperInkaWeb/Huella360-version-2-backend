package com.vet_saas.modules.subscription.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pago de Mercado Pago ya aplicado a una suscripcion. Existe para que un mismo
 * pago no pueda activar/extender el plan mas de una vez (ver V50).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "suscripcion_pagos")
public class SuscripcionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscripcion_pago")
    private Long id;

    @Column(name = "mp_payment_id", nullable = false, unique = true, length = 100)
    private String mpPaymentId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "veterinario_id")
    private Long veterinarioId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }
}
