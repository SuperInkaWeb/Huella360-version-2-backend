package com.vet_saas.modules.delivery.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.delivery.dto.request.UbicacionDTO;
import com.vet_saas.modules.delivery.dto.response.UbicacionEvent;
import com.vet_saas.modules.delivery.model.Delivery;
import com.vet_saas.modules.delivery.model.DeliveryStatus;
import com.vet_saas.modules.delivery.model.TrackingRepartidor;
import com.vet_saas.modules.delivery.repository.DeliveryRepository;
import com.vet_saas.modules.delivery.repository.RepartidorRepository;
import com.vet_saas.modules.delivery.repository.TrackingRepartidorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackingService {

    private final DeliveryRepository deliveryRepository;
    private final TrackingRepartidorRepository trackingRepository;
    private final RepartidorRepository repartidorRepository;
    private final DeliveryService deliveryService;

    // Umbral para cambiar a estado CERCA (500 metros)
    private static final double UMBRAL_CERCA_KM = 0.5;

    /**
     * Procesa una posicion GPS del repartidor:
     * 1. Actualiza ubicacion del repartidor en BD
     * 2. Persiste punto de tracking
     * 3. Verifica si esta cerca del destino → cambia estado
     * 4. Retorna evento para broadcast WebSocket
     */
    @Transactional
    public UbicacionEvent procesarUbicacion(Long deliveryId, UbicacionDTO pos, Long repartidorId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        // Actualizar ubicacion del repartidor (query optimizado, no UPDATE completo)
        repartidorRepository.actualizarUbicacion(repartidorId, pos.getLat(), pos.getLng());

        // Persistir punto de tracking en BD
        trackingRepository.save(TrackingRepartidor.builder()
            .delivery(delivery)
            .repartidor(delivery.getRepartidor())
            .lat(pos.getLat())
            .lng(pos.getLng())
            .velocidadKmh(pos.getVelocidadKmh())
            .registradoAt(Instant.now())
            .build());

        // Verificar si el repartidor esta cerca del destino
        if (delivery.getEstado() == DeliveryStatus.EN_CAMINO
                && pos.getDistanciaAlDestinoKm() != null
                && pos.getDistanciaAlDestinoKm() <= UMBRAL_CERCA_KM) {

            deliveryService.cambiarEstado(deliveryId, DeliveryStatus.CERCA,
                repartidorId, "Repartidor a menos de 500m del destino");
        }

        // Calcular tiempo estimado simple (velocidad promedio 30km/h en ciudad)
        Integer tiempoEstimado = null;
        if (pos.getDistanciaAlDestinoKm() != null) {
            double velocidad = pos.getVelocidadKmh() != null
                ? pos.getVelocidadKmh().doubleValue()
                : 25.0; // velocidad promedio default
            tiempoEstimado = (int) Math.ceil((pos.getDistanciaAlDestinoKm() / velocidad) * 60);
        }

        return UbicacionEvent.builder()
            .deliveryId(deliveryId)
            .lat(pos.getLat())
            .lng(pos.getLng())
            .velocidadKmh(pos.getVelocidadKmh())
            .tiempoEstimadoMin(tiempoEstimado)
            .distanciaRestanteKm(pos.getDistanciaAlDestinoKm())
            .timestamp(Instant.now())
            .build();
    }

    /** Distancia Haversine entre dos puntos (en km) */
    public static double calcularDistanciaKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
