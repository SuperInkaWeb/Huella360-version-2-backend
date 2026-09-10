package com.vet_saas.modules.notification.listener;

import com.vet_saas.modules.delivery.event.DeliveryStatusChangedEvent;
import com.vet_saas.modules.delivery.model.DeliveryStatus;
import com.vet_saas.modules.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeliveryStatusChangedListener {

    private final EmailService emailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        log.info("Procesando notificación email: delivery {} cambió de {} a {}",
            event.getDeliveryId(), event.getEstadoAnterior(), event.getEstadoNuevo());

        try {
            String emailCliente = event.getEmailCliente();
            if (emailCliente == null || emailCliente.isBlank()) {
                log.warn("No se envía notificación: cliente sin email para delivery {}", event.getDeliveryId());
                return;
            }

            switch (event.getEstadoNuevo()) {
                case REPARTIDOR_ASIGNADO -> emailService.sendDeliveryAsignadoEmail(
                    event.getOrdenId(), event.getNombreRepartidor());
                case EN_CAMINO -> emailService.sendDeliveryEnCaminoEmail(
                    event.getOrdenId());
                case ENTREGADO -> emailService.sendDeliveryEntregadoEmail(
                    event.getOrdenId());
                case FALLIDO -> emailService.sendDeliveryFallidoEmail(
                    event.getOrdenId(), event.getDescripcion());
                case CANCELADO -> emailService.sendDeliveryCanceladoEmail(
                    event.getOrdenId(), event.getDescripcion());
                default -> log.debug("No se envía email para estado {}", event.getEstadoNuevo());
            }
        } catch (Exception e) {
            log.error("Error enviando notificación email para delivery {}: {}",
                event.getDeliveryId(), e.getMessage());
        }
    }
}
