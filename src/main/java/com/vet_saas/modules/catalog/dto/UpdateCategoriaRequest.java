package com.vet_saas.modules.catalog.dto;

public record UpdateCategoriaRequest(
        String nombre,
        Long padreId,
        String iconoUrl,
        Boolean activo,
        Integer orden) {
}
