package com.vet_saas.modules.referral.model;

import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "referidos")
public class Referido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_referido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_que_refirio_id", nullable = false)
    private Usuario usuarioQueRefirio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_refirido_id", nullable = false, unique = true)
    private Usuario usuarioRefirido;

    @Column(name = "codigo_referido")
    private String codigoReferido;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
