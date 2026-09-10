package com.vet_saas.modules.pet.dto;

import com.vet_saas.modules.pet.model.Sexo;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePetDto(
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento,

        @Positive(message = "El peso debe ser un valor positivo")
        BigDecimal pesoKg,

        Boolean esterilizado,
        String observacionesMedicas
) {}
