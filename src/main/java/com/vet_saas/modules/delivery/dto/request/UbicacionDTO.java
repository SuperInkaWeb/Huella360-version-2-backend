package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UbicacionDTO {

    @NotNull
    private BigDecimal lat;

    @NotNull
    private BigDecimal lng;

    private BigDecimal velocidadKmh;

    // Distancia al destino en km (calculada en el cliente con GPS)
    private Double distanciaAlDestinoKm;
}
