package com.vet_saas.modules.adoption.dto;

import com.vet_saas.modules.adoption.model.EstadoAdopcion;

import java.time.LocalDateTime;

public record AdoptionResponse(
                Long id,
                Long mascotaId,
                String mascotaNombre,
                String mascotaFotoUrl,
                String titulo,
                String historia,
                String requisitos,
                String ubicacionCiudad,
                EstadoAdopcion estado,
                Long publicadoPorId,
                String publicadoPorNombre,
                String publicadoPorTipoServicio,
                LocalDateTime fechaPublicacion) {
}
