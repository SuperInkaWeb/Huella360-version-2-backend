package com.vet_saas.modules.company.dto;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateCompanyDto(
                String nombreComercial,
                String descripcion,
                String tipoServicio,

                @Pattern(regexp = "\\d+", message = "El teléfono solo debe contener números") String telefono,

                @Email(message = "Formato de email inválido") String emailContacto,

                String direccion,
                String ciudad,

                BigDecimal latitud,
                BigDecimal longitud) {
}