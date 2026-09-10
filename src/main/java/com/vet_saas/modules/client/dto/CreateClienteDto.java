package com.vet_saas.modules.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Pattern;

public record CreateClienteDto(
                @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombres,

                @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String apellidos,

                @Size(max = 20) @Pattern(regexp = "\\d+", message = "El teléfono solo debe contener números") String telefono,

                @Size(max = 255) String direccion,

                @Size(max = 100) String ciudad,

                @Size(max = 100) String pais) {
}
