package com.vet_saas.modules.delivery.event;

import com.vet_saas.modules.delivery.model.DeliveryStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryStatusChangedEvent extends ApplicationEvent {

    private final Long deliveryId;
    private final Long ordenId;
    private final DeliveryStatus estadoAnterior;
    private final DeliveryStatus estadoNuevo;
    private final String descripcion;
    private final String nombreRepartidor;
    private final String emailCliente;

    public DeliveryStatusChangedEvent(
            Object source,
            Long deliveryId,
            Long ordenId,
            DeliveryStatus estadoAnterior,
            DeliveryStatus estadoNuevo,
            String descripcion,
            String nombreRepartidor,
            String emailCliente) {
        super(source);
        this.deliveryId = deliveryId;
        this.ordenId = ordenId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.descripcion = descripcion;
        this.nombreRepartidor = nombreRepartidor;
        this.emailCliente = emailCliente;
    }
}
