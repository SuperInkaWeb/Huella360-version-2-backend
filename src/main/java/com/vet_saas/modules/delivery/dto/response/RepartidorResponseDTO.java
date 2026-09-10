package com.vet_saas.modules.delivery.dto.response;

import com.vet_saas.modules.delivery.model.RepartidorStatus;
import com.vet_saas.modules.delivery.model.VehicleType;
import com.vet_saas.modules.delivery.model.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class RepartidorResponseDTO {
    private Long idRepartidor;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String fotoPerfil;
    private VehicleType tipoVehiculo;
    private String placaVehiculo;
    private RepartidorStatus estadoActual;
    private VerificationStatus estadoValidacion;
    private BigDecimal calificacionPromedio;
    private Integer totalEntregas;
    private BigDecimal ubicacionLat;
    private BigDecimal ubicacionLng;
    private Instant ultimaUbicacionAt;
    private Boolean activo;
}
