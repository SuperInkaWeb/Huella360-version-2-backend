package com.vet_saas.modules.delivery.dto.response;

import com.vet_saas.modules.delivery.model.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class DeliveryEmpresaResponseDTO {
    private Long idDelivery;
    private Long ordenId;
    private String codigoOrden;
    private DeliveryStatus estado;
    private String clienteNombre;
    private String clienteTelefono;
    private Integer totalUnidades;
    private List<String> productos;
    private Long repartidorId;
    private String repartidorNombre;
    private String repartidorTelefono;
    private String repartidorVehiculo;
    private String repartidorPlaca;
    private BigDecimal repartidorLat;
    private BigDecimal repartidorLng;
    private BigDecimal origenLat;
    private BigDecimal origenLng;
    private String origenDireccion;
    private BigDecimal destinoLat;
    private BigDecimal destinoLng;
    private String destinoDireccion;
    private String destinoReferencia;
    private String fotoEntregaUrl;
    private Instant createdAt;
    private Instant asignadoAt;
    private Instant entregadoAt;
    private Instant empresaConfirmadoAt;
}
