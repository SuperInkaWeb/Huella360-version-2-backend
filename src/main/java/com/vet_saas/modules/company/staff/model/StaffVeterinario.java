package com.vet_saas.modules.company.staff.model;

import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "staff_veterinario", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "veterinario_id"})
})
public class StaffVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_staff")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @Column(name = "rol_interno")
    private String rolInterno;

    // CAMBIO IMPORTANTE: Mapeo del Enum de Postgres
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "staff_status", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class) // Opcional si usas Hibernate 6, ayuda a mapear directo
    private StaffStatus estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) estado = StaffStatus.PENDIENTE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}