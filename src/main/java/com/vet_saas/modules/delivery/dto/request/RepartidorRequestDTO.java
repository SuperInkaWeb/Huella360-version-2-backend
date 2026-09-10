package com.vet_saas.modules.delivery.dto.request;

import com.vet_saas.modules.delivery.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartidorRequestDTO {
    
    @NotBlank(message = "Los nombres son requeridos")
    private String nombres;
    
    @NotBlank(message = "Los apellidos son requeridos")
    private String apellidos;
    
    @NotBlank(message = "El teléfono es requerido")
    private String telefono;
    
    @NotNull(message = "El tipo de vehículo es requerido")
    private VehicleType tipoVehiculo;
    
    @NotBlank(message = "La placa del vehículo es requerida")
    private String placaVehiculo;
}
