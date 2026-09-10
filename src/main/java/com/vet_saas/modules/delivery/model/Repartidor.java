package com.vet_saas.modules.delivery.model;

import com.vet_saas.modules.user.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "repartidores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repartidor")
    private Long idRepartidor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 20, unique = true)
    private String dni;

    @Column(name = "foto_perfil_url")
    private String fotoPerfil;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_vehiculo", columnDefinition = "vehicle_type")
    @Builder.Default
    private VehicleType tipoVehiculo = VehicleType.MOTO;

    @Column(name = "placa_vehiculo", length = 20)
    private String placaVehiculo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_validacion", columnDefinition = "verification_status")
    @Builder.Default
    private VerificationStatus estadoValidacion = VerificationStatus.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_actual", columnDefinition = "repartidor_status")
    @Builder.Default
    private RepartidorStatus estadoActual = RepartidorStatus.OFFLINE;

    @Column(name = "ubicacion_lat", precision = 10, scale = 8)
    private BigDecimal ubicacionLat;

    @Column(name = "ubicacion_lng", precision = 11, scale = 8)
    private BigDecimal ubicacionLng;

    @Column(name = "ultima_ubicacion_at")
    private Instant ultimaUbicacionAt;

    @Column(name = "calificacion_promedio", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal calificacionPromedio = new BigDecimal("5.00");

    @Column(name = "total_entregas")
    @Builder.Default
    private Integer totalEntregas = 0;

    @Column(name = "max_pedidos_simultaneos", nullable = false)
    @Builder.Default
    private Integer maxPedidosSimultaneos = 3;

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
