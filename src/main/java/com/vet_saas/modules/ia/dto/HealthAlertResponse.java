package com.vet_saas.modules.ia.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HealthAlertResponse {
    private List<HealthAlert> alertas;
    private String resumen;
    private int alertasGeneradas;

    @Data
    @Builder
    public static class HealthAlert {
        private String tipo;
        private String severidad;
        private String titulo;
        private String descripcion;
        private String recomendacion;
    }
}
