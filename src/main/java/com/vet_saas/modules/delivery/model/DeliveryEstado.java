package com.vet_saas.modules.delivery.model;

import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "delivery_estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "delivery_status")
    private DeliveryStatus estado;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_by")
    private Usuario registradoBy;

    @Column(name = "registrado_at")
    @Builder.Default
    private Instant registradoAt = Instant.now();
}
