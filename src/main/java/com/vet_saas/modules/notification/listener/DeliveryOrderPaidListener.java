package com.vet_saas.modules.notification.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vet_saas.modules.delivery.dto.request.CrearDeliveryDTO;
import com.vet_saas.modules.delivery.service.DeliveryService;
import com.vet_saas.modules.sales.event.OrderPaidEvent;
import com.vet_saas.modules.sales.model.Orden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeliveryOrderPaidListener {

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        Orden orden = event.getOrden();
        log.info("Evento recibido: Creando delivery automático para orden {}", orden.getCodigoOrden());

        try {
            // 1. Validar si requiere delivery (si costo de envío es nulo o 0, no hay delivery)
            if (orden.getCostoEnvio() == null || orden.getCostoEnvio().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("La orden {} no requiere delivery.", orden.getCodigoOrden());
                return;
            }

            CrearDeliveryDTO dto = new CrearDeliveryDTO();

            // 2. Coordenadas de Origen (La Empresa)
            if (orden.getEmpresa() != null) {
                dto.setOrigenLat(orden.getEmpresa().getUbicacionLat());
                dto.setOrigenLng(orden.getEmpresa().getUbicacionLng());
                dto.setOrigenDireccion(orden.getEmpresa().getDireccion());
            } else {
                log.warn("Orden {} no tiene empresa, no se puede originar el delivery.", orden.getCodigoOrden());
                return;
            }

            // 3. Coordenadas de Destino (Extrayendo del JSONB)
            // Asegúrate de que las keys "lat", "lng", "direccion" coincidan con cómo lo guardas en la orden
            if (orden.getDireccionEnvio() != null) {
                Map<String, Object> direccion = objectMapper.convertValue(orden.getDireccionEnvio(), new TypeReference<>() {});

                dto.setDestinoLat(new BigDecimal(direccion.get("lat").toString()));
                dto.setDestinoLng(new BigDecimal(direccion.get("lng").toString()));
                dto.setDestinoDireccion(direccion.get("direccion").toString());

                if (direccion.containsKey("referencia") && direccion.get("referencia") != null) {
                    dto.setDestinoReferencia(direccion.get("referencia").toString());
                }
            } else {
                log.error("La orden {} no tiene dirección de envío.", orden.getCodigoOrden());
                return;
            }

            dto.setCostoDelivery(orden.getCostoEnvio());

            // 4. Crear el Delivery
            deliveryService.crearDelivery(orden, dto);
            log.info("Delivery creado con éxito para la orden {}", orden.getCodigoOrden());

        } catch (Exception e) {
            log.error("Error al crear delivery automático para la orden {}: {}", orden.getCodigoOrden(), e.getMessage(), e);
        }
    }
}