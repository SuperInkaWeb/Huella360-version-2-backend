package com.vet_saas.modules.geo.dto;

import java.math.BigDecimal;

/**
 * Empresa o veterinario independiente cercano a un punto.
 *
 * @param tipo        EMPRESA o VETERINARIO
 * @param categoria   tipo de servicio de la empresa (VETERINARIA, PETSHOP...) o especialidad del veterinario
 * @param imagenUrl   logo de la empresa o foto del veterinario
 * @param distanciaKm distancia en línea recta al punto buscado, con 2 decimales
 */
public record LugarCercanoResponse(
        String tipo,
        Long id,
        String nombre,
        String categoria,
        String direccion,
        String ciudad,
        String imagenUrl,
        BigDecimal latitud,
        BigDecimal longitud,
        double distanciaKm) {
}
