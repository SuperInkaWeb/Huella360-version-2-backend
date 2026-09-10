package com.vet_saas.modules.catalog.model;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Veterinario veterinario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion_minutos")
    @Builder.Default
    private Integer duracionMinutos = 30;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_modality")
    @Builder.Default
    private ModalidadServicio modalidad = ModalidadServicio.PRESENCIAL;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean visible = true;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Version
    private Long version;

    public boolean perteneceAEmpresa() {
        return empresa != null;
    }

    public boolean perteneceAVeterinario() {
        return veterinario != null;
    }

    @PrePersist
    @PreUpdate
    protected void validateOwnership() {
        if ((empresa == null && veterinario == null) || (empresa != null && veterinario != null)) {
            throw new BusinessException(
                    "Un servicio debe pertenecer exactamente a una Empresa o a un Veterinario independiente, nunca a ambos ni a ninguno.");
        }
    }
}
