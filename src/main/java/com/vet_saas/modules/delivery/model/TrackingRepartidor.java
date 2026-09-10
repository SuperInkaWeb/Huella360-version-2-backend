package com.vet_saas.modules.delivery.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tracking_repartidor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingRepartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Repartidor repartidor;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal lng;

    @Column(name = "velocidad_kmh", precision = 5, scale = 2)
    private BigDecimal velocidadKmh;

    @Column(name = "registrado_at")
    @Builder.Default
    private Instant registradoAt = Instant.now();
}
