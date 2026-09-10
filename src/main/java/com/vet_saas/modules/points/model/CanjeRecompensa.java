package com.vet_saas.modules.points.model;

import com.vet_saas.modules.sales.model.Orden;
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
@Table(name = "canjes_recompensas")
public class CanjeRecompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PuntosCliente puntosCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recompensa", nullable = false)
    private Recompensa recompensa;

    @Column(name = "fecha_canje", nullable = false, updatable = false)
    private LocalDateTime fechaCanje;

    @Column(nullable = false)
    private Boolean utilizado = false;

    @Column(name = "fecha_utilizacion")
    private LocalDateTime fechaUtilizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    @PrePersist
    protected void onCreate() {
        fechaCanje = LocalDateTime.now();
    }
}
