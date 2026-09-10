package com.vet_saas.modules.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCompanyDto(
                @NotBlank(message = "El nombre comercial es obligatorio") String nombreComercial,

                @NotBlank(message = "La razón social es obligatoria") String razonSocial,

                @NotBlank(message = "El RUC es obligatorio") @Size(min = 11, max = 11, message = "El RUC debe tener 11 dígitos") @Pattern(regexp = "\\d+", message = "El RUC solo debe contener números") String ruc,

                String descripcion,

                @NotBlank(message = "El tipo de servicio es obligatorio (VETERINARIA, PETSHOP, ETC)") String tipoServicio,

                @NotBlank(message = "El teléfono es obligatorio") @Pattern(regexp = "\\d+", message = "El teléfono solo debe contener números") String telefono,

                @NotBlank(message = "El email de contacto es obligatorio") @Email(message = "Formato de email inválido") String emailContacto,

                @NotBlank(message = "La dirección es obligatoria") String direccion,

                @NotBlank(message = "La ciudad es obligatoria") String ciudad,

                BigDecimal latitud,
                BigDecimal longitud) {
}