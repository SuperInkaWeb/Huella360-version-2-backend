package com.vet_saas.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank(message = "El rol es requerido")
        @Pattern(regexp = "CLIENTE|EMPRESA|VETERINARIO|REPARTIDOR", message = "Rol inválido. Debe ser CLIENTE, EMPRESA, VETERINARIO o REPARTIDOR")
        String rol
) {}
