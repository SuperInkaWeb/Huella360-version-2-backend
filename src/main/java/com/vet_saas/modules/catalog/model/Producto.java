package com.vet_saas.modules.catalog.model;

import com.vet_saas.modules.company.model.Empresa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_oferta", precision = 10, scale = 2)
    private BigDecimal precioOferta;

    @Column(name = "stock_actual", nullable = false)
    private Integer stock;

    private String sku;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    @Column(name = "oferta_inicio")
    private LocalDateTime ofertaInicio;

    @Column(name = "oferta_fin")
    private LocalDateTime ofertaFin;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean visible = true;

    @Version
    private Long version;

    // Mapea la columna JSONB de Postgres a una Lista de Strings en Java
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "imagenes", columnDefinition = "jsonb")
    private List<String> imagenes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null)
            estado = EstadoProducto.ACTIVO;
        if (stock == null)
            stock = 0;
        if (activo == null)
            activo = true;
        if (visible == null)
            visible = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getPrecioActual() {
        LocalDateTime now = LocalDateTime.now();
        if (precioOferta != null && ofertaInicio != null && ofertaFin != null) {
            if (!now.isBefore(ofertaInicio) && !now.isAfter(ofertaFin)) {
                return precioOferta;
            }
        }
        return precio;
    }
}