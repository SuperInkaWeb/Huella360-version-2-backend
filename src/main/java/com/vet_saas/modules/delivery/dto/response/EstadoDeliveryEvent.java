package com.vet_saas.modules.delivery.dto.response;

import com.vet_saas.modules.delivery.model.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento de cambio de estado enviado por WebSocket */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDeliveryEvent {
    private Long deliveryId;
    private DeliveryStatus estado;
    private String descripcion;
    private Instant timestamp;
}

