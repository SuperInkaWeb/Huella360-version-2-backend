package com.vet_saas.modules.company.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddStaffDto(
        @NotBlank(message = "El email del veterinario es obligatorio")
        @Email(message = "Formato de email inválido")
        String emailVeterinario,

        @NotBlank(message = "El rol interno es obligatorio (Ej: Cirujano)")
        String rolInterno
) {}