package com.vet_saas.modules.admin.dto;

import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVeterinarioResponseDto {
    private Long id;
    private String nombres;
    private String apellidos;
    private String correo;
    private String especialidad;
    private String numeroColegiatura;
    private String biografia;
    private Integer aniosExperiencia;
    private String fotoPerfilUrl;
    private VerificationStatus estadoValidacion;
    private boolean usuarioActivo;
    private LocalDateTime createdAt;
}
