package com.vet_saas.modules.admin.dto;

import com.vet_saas.modules.user.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDto {
    private Long id;
    private String correo;
    private Role rol;
    private boolean estado;
    private boolean emailVerificado;
    private LocalDateTime createdAt;
    private String nombre; // Can be derived from profile
}
