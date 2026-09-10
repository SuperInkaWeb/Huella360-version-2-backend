package com.vet_saas.modules.pet.dto;

import com.vet_saas.modules.pet.model.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePetDto(
        @NotBlank(message = "El nombre de la mascota es obligatorio") String nombre,

        @NotBlank(message = "La especie es obligatoria") String especie,

        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento,

        @Positive(message = "El peso debe ser un valor positivo") BigDecimal pesoKg,

        Boolean esterilizado,
        String observacionesMedicas) {
}
