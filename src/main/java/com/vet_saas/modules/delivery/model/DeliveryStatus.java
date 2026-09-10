package com.vet_saas.modules.delivery.model;

import java.util.EnumSet;

public enum DeliveryStatus {
    BUSCANDO_REPARTIDOR,
    REPARTIDOR_ASIGNADO,
    EN_TIENDA,
    RECOGIDO,
    EN_CAMINO,
    CERCA,
    ENTREGADO,
    FALLIDO,
    CANCELADO,
    INCIDENCIA;
    public boolean puedeTransicionarA(DeliveryStatus siguiente) {
        // Permitir error o incidencia desde cualquier estado intermedio
        if (siguiente == INCIDENCIA || siguiente == FALLIDO) {
            return !esFinal();
        }
        
        // RECUPERACION: Permitir que administración resetee una incidencia
        if (this == INCIDENCIA && siguiente == BUSCANDO_REPARTIDOR) {
            return true;
        }

        return switch (this) {
            case BUSCANDO_REPARTIDOR -> EnumSet.of(REPARTIDOR_ASIGNADO, FALLIDO, CANCELADO).contains(siguiente);
            case REPARTIDOR_ASIGNADO -> EnumSet.of(EN_TIENDA, CANCELADO, BUSCANDO_REPARTIDOR).contains(siguiente);
            case EN_TIENDA           -> EnumSet.of(RECOGIDO, CANCELADO, BUSCANDO_REPARTIDOR).contains(siguiente);
            case RECOGIDO            -> EnumSet.of(EN_CAMINO).contains(siguiente);
            case EN_CAMINO           -> EnumSet.of(CERCA, ENTREGADO).contains(siguiente);
            case CERCA               -> EnumSet.of(ENTREGADO, FALLIDO).contains(siguiente);
            default                  -> false;
        };
    }

    public boolean esFinal() {
        return this == ENTREGADO || this == FALLIDO || this == CANCELADO || this == INCIDENCIA;
    }
}
