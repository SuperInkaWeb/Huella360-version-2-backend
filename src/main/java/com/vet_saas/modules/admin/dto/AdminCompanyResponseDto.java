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
public class AdminCompanyResponseDto {
    private Long id;
    private String nombreComercial;
    private String ruc;
    private String emailContacto;
    private String tipoServicio;
    private String telefonoContacto;
    private String direccion;
    private String ciudad;
    private String pais;
    private VerificationStatus estadoValidacion;
    private LocalDateTime createdAt;
    private String ownerEmail;
}
