package com.vet_saas.modules.delivery.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrearDeliveryDTO {

    @NotNull(message = "La latitud de origen es requerida")
    private BigDecimal origenLat;

    @NotNull(message = "La longitud de origen es requerida")
    private BigDecimal origenLng;

    private String origenDireccion;

    @NotNull(message = "La latitud de destino es requerida")
    private BigDecimal destinoLat;

    @NotNull(message = "La longitud de destino es requerida")
    private BigDecimal destinoLng;

    @NotBlank(message = "La direccion de destino es requerida")
    private String destinoDireccion;

    private String destinoReferencia;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal costoDelivery;
}
