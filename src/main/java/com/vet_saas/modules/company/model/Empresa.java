package com.vet_saas.modules.company.model;

import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_propietario_id", referencedColumnName = "id_usuario", nullable = false)
    private Usuario usuarioPropietario;

    @Column(name = "nombre_comercial", nullable = false)
    private String nombreComercial;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(unique = true)
    private String ruc;

    private String descripcion;

    @Column(name = "tipo_servicio")
    private String tipoServicio;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Column(name = "email_contacto")
    private String emailContacto;

    private String direccion;
    private String ciudad;
    private String pais;

    @Column(name = "ubicacion_lat", precision = 10, scale = 8)
    private BigDecimal ubicacionLat;

    @Column(name = "ubicacion_lng", precision = 11, scale = 8)
    private BigDecimal ubicacionLng;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "mp_access_token")
    private String mpAccessToken;

    @Column(name = "mp_public_key")
    private String mpPublicKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_validacion", columnDefinition = "verification_status")
    private VerificationStatus estadoValidacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documentos_url", columnDefinition = "jsonb")
    private Map<String, String> documentosUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estadoValidacion == null)
            estadoValidacion = VerificationStatus.PENDIENTE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}