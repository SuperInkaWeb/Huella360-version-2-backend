package com.vet_saas.modules.delivery.model;

import com.vet_saas.modules.sales.model.Orden;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_delivery")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private Repartidor repartidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id")
    private ZonaCobertura zona;

    // ---- Origen (tienda) ----
    @Column(name = "origen_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal origenLat;

    @Column(name = "origen_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal origenLng;

    @Column(name = "origen_direccion")
    private String origenDireccion;

    // ---- Destino (cliente) ----
    @Column(name = "destino_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal destinoLat;

    @Column(name = "destino_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal destinoLng;

    @Column(name = "destino_direccion", nullable = false)
    private String destinoDireccion;

    @Column(name = "destino_referencia")
    private String destinoReferencia;

    // ---- Estado ----
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "delivery_status")
    @Builder.Default
    private DeliveryStatus estado = DeliveryStatus.BUSCANDO_REPARTIDOR;

    @Column(name = "distancia_km", precision = 6, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "tiempo_estimado_min")
    private Integer tiempoEstimadoMin;

    @Column(name = "costo_delivery", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoDelivery = BigDecimal.ZERO;

    // ---- Timestamps por fase (analytics) ----
    @Column(name = "asignado_at")
    private Instant asignadoAt;

    @Column(name = "en_tienda_at")
    private Instant enTiendaAt;

    @Column(name = "recogido_at")
    private Instant recogidoAt;

    @Column(name = "entregado_at")
    private Instant entregadoAt;

    // ---- OTP (hash BCrypt, nunca plano) ----
    @Column(name = "codigo_confirmacion")
    private String codigoConfirmacion;

    @Column(name = "codigo_expira_at")
    private Instant codigoExpiraAt;

    // ---- Foto de entrega (Cloudinary) ----
    @Column(name = "foto_entrega_url")
    private String fotoEntregaUrl;

    // ---- Calificaciones ----
    // SMALLINT en PostgreSQL → Short en Java (no Integer)
    @Column(name = "calificacion_cliente", columnDefinition = "SMALLINT")
    private Short calificacionCliente;

    @Column(name = "calificacion_repartidor", columnDefinition = "SMALLINT")
    private Short calificacionRepartidor;

    @Column(name = "calificacion_producto", columnDefinition = "SMALLINT")
    private Short calificacionProducto;

    @Column(name = "comentario_cliente")
    private String comentarioCliente;

    @Column(name = "comentario_producto")
    private String comentarioProducto;

    @Column(name = "intentos_asignacion")
    @Builder.Default
    private Integer intentosAsignacion = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
