package com.vet_saas.modules.sales.event;

import com.vet_saas.modules.sales.model.Orden;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderPaidEvent extends ApplicationEvent {
    private final Orden orden;

    public OrderPaidEvent(Object source, Orden orden) {
        super(source);
        this.orden = orden;
    }
}