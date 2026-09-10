package com.vet_saas.modules.ia.model;

import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ia_usage", indexes = {
    @Index(name = "idx_ia_usage_usuario", columnList = "usuario_id"),
    @Index(name = "idx_ia_usage_usuario_fecha", columnList = "usuario_id, fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IaUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "mascota_id")
    private Long mascotaId;

    @Column(name = "tokens_usados")
    private Integer tokensUsados;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private boolean exitoso;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fecha;
}
