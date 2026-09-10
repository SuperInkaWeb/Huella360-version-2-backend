package com.vet_saas.modules.points.model;

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
@Table(name = "historial_puntos")
public class HistorialPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PuntosCliente puntosCliente;

    @Column(nullable = false)
    private Integer puntos;

    @Column(name = "tipo_accion", nullable = false, length = 100)
    private String tipoAccion;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
