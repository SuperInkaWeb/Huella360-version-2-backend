package com.vet_saas.modules.delivery.dto.response;

import com.vet_saas.modules.delivery.model.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class DeliveryResponseDTO {

    private Long idDelivery;
    private Long ordenId;
    private DeliveryStatus estado;

    // Datos del Cliente
    private String clienteNombre;
    private String clienteTelefono;

    // Repartidor (null si aún no asignado)
    private Long repartidorId;
    private String repartidorNombre;
    private String repartidorFoto;
    private String repartidorTelefono;
    private String repartidorVehiculo;
    private BigDecimal repartidorCalificacionPromedio;

    // Ubicacion actual del repartidor (puede ser null)
    private BigDecimal repartidorLat;
    private BigDecimal repartidorLng;

    // Destino
    private BigDecimal destinoLat;
    private BigDecimal destinoLng;
    private String destinoDireccion;
    private String destinoReferencia;

    // Origen
    private BigDecimal origenLat;
    private BigDecimal origenLng;
    private String origenDireccion;

    // Tiempos
    private Integer tiempoEstimadoMin;
    private BigDecimal distanciaKm;
    private BigDecimal costoDelivery;

    // Solo se envía al cliente una vez (al crear el delivery)
    // En requests GET normales viene null por seguridad
    private String otpCliente;

    // Calificaciones (vienen del cliente)
    private Short calificacionRepartidor;
    private String comentarioRepartidor;
    private Short calificacionProducto;
    private String comentarioProducto;

    // Foto de entrega (disponible tras ENTREGADO)
    private String fotoEntregaUrl;

    // Timestamps de fases
    private Instant asignadoAt;
    private Instant recogidoAt;
    private Instant entregadoAt;
    private Instant createdAt;
}
