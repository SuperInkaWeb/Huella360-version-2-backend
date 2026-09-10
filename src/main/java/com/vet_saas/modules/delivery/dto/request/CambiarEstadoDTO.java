package com.vet_saas.modules.delivery.dto.request;

import com.vet_saas.modules.delivery.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoDTO {

    @NotNull(message = "El nuevo estado es requerido")
    private DeliveryStatus nuevoEstado;

    private String descripcion;
}
