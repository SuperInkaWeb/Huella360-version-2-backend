package com.vet_saas.modules.client.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateClienteDto(
                @Size(max = 100) String nombres,

                @Size(max = 100) String apellidos,

                @Size(max = 20) @Pattern(regexp = "\\d+", message = "El teléfono solo debe contener números") String telefono,

                @Size(max = 255) String direccion,

                @Size(max = 100) String ciudad,

                @Size(max = 100) String pais,

                @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90") @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90") BigDecimal latitud,

                @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180") @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180") BigDecimal longitud) {
}
