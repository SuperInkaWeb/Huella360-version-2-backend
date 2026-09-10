package com.vet_saas.modules.sales.model;

import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long id;

    @Column(name = "codigo_orden", nullable = false, unique = true)
    private String codigoOrden; // Ej: ORD-2026-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cliente_id")
    private Usuario usuarioCliente;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_nombre")
    private String guestNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Veterinario veterinario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "costo_envio", precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(name = "comision_plataforma", precision = 10, scale = 2)
    private BigDecimal comisionPlataforma;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "order_status")
    private EstadoOrden estado;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "mp_preference_id")
    private String mpPreferenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "direccion_envio", columnDefinition = "jsonb")
    private Map<String, Object> direccionEnvio;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrden> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estado == null)
            estado = EstadoOrden.PENDIENTE;
        if (costoEnvio == null)
            costoEnvio = BigDecimal.ZERO;
        if (comisionPlataforma == null)
            comisionPlataforma = BigDecimal.ZERO;
        if (descuento == null)
            descuento = BigDecimal.ZERO;

        // Validar que tenga al menos un vendor
        if (empresa == null && veterinario == null) {
            throw new IllegalStateException("La orden debe tener una Empresa o un Veterinario asociado");
        }
        // Validar que tenga usuario autenticado o datos de guest
        if (usuarioCliente == null && guestEmail == null) {
            throw new IllegalStateException("La orden debe tener un usuario autenticado o datos de guest (email)");
        }
    }
}