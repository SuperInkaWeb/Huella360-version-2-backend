package com.vet_saas.modules.company.dto;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateCompanyDto(
                String nombreComercial,
                String descripcion,
                String tipoServicio,

                @Pattern(regexp = "\\d{6,15}", message = "El teléfono debe tener entre 6 y 15 dígitos, solo números") String telefono,

                @Email(message = "Formato de email inválido") String emailContacto,

                String direccion,
                String ciudad,

                BigDecimal latitud,
                BigDecimal longitud) {
}