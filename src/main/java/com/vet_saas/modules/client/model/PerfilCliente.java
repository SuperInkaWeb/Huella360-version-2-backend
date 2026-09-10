package com.vet_saas.modules.client.model;

import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perfiles_clientes")
public class PerfilCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    private String telefono;
    private String direccion;
    private String ciudad;

    @Column(columnDefinition = "VARCHAR(100) DEFAULT 'Perú'")
    private String pais;

    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;

    @Column(name = "ubicacion_lat", precision = 10, scale = 8)
    private BigDecimal ubicacionLat;

    @Column(name = "ubicacion_lng", precision = 11, scale = 8)
    private BigDecimal ubicacionLng;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}