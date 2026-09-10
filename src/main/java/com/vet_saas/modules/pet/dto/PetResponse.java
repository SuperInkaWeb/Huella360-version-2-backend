package com.vet_saas.modules.pet.dto;

import com.vet_saas.modules.pet.model.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetResponse(
        Long id,
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento,
        BigDecimal pesoKg,
        String fotoUrl,
        Boolean esterilizado,
        String observacionesMedicas,
        LocalDateTime createdAt) {
}
