package com.vet_saas.modules.catalog.repository;

/**
 * Proyección: cantidad de productos públicos (activos y visibles) asignados directamente a una categoría.
 */
public interface CategoriaProductCount {

    Long getCategoriaId();

    Long getTotal();
}
