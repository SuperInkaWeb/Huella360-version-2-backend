package com.vet_saas.modules.delivery.controller;

import com.vet_saas.modules.delivery.dto.request.UbicacionDTO;
import com.vet_saas.modules.delivery.dto.response.UbicacionEvent;
import com.vet_saas.modules.delivery.service.TrackingService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class TrackingWsController {

    private final TrackingService trackingService;
    private final SimpMessagingTemplate wsTemplate;

    /**
     * El repartidor envía su GPS cada ~5 segundos:
     *   SEND /app/tracking/{deliveryId}/ubicacion
     *
     * El cliente se suscribe a:
     *   SUBSCRIBE /topic/delivery/{deliveryId}/ubicacion
     */
    @MessageMapping("/tracking/{deliveryId}/ubicacion")
    public void recibirUbicacion(
            @DestinationVariable Long deliveryId,
            @Payload UbicacionDTO pos,
            Authentication auth) {

        Usuario principal = (Usuario) auth.getPrincipal();

        UbicacionEvent evento = trackingService.procesarUbicacion(
            deliveryId, pos, principal.getId()
        );

        // Broadcast a todos los suscritos (cliente, empresa, etc.)
        wsTemplate.convertAndSend(
            "/topic/delivery/" + deliveryId + "/ubicacion",
            evento
        );

        log.debug("GPS delivery {}: lat={}, lng={}", deliveryId, pos.getLat(), pos.getLng());
    }
}
