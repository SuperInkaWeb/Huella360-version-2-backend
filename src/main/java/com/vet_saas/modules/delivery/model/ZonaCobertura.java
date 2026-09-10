package com.vet_saas.modules.delivery.model;

import com.vet_saas.modules.company.model.Empresa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "zonas_cobertura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaCobertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zona")
    private Long idZona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "radio_km", precision = 5, scale = 2)
    private BigDecimal radioKm;

    @Column(name = "costo_envio", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Builder.Default
    private Boolean activo = true;
}
