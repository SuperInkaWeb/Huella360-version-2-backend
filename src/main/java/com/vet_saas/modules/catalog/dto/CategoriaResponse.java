package com.vet_saas.modules.catalog.dto;

public record CategoriaResponse(
        Long id,
        String nombre,
        String slug,
        Long padreId,
        String padreNombre,
        String iconoUrl,
        Boolean activo,
        Integer orden) {
}
