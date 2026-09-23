package com.vet_saas.modules.catalog.dto;

import java.util.List;

/**
 * Categoría del marketplace con su conteo de productos públicos.
 * En una categoría padre, {@code productCount} incluye los productos de sus subcategorías.
 */
public record MarketplaceCategoriaResponse(
        Long id,
        String nombre,
        String slug,
        String iconoUrl,
        long productCount,
        List<MarketplaceCategoriaResponse> subcategorias) {
}
