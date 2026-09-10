package com.vet_saas.modules.notification.listener;

import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.sales.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final EmailService emailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Evento recibido: Enviando email para orden {}", event.getOrden().getCodigoOrden());
        emailService.sendOrderConfirmation(event.getOrden().getId());
    }
}