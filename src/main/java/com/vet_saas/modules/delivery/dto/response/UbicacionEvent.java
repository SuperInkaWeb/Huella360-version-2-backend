package com.vet_saas.modules.delivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Posicion GPS enviada por WebSocket al cliente que sigue el delivery */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionEvent {
    private Long deliveryId;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal velocidadKmh;
    private Integer tiempoEstimadoMin;
    private Double distanciaRestanteKm;
    private Instant timestamp;
}
