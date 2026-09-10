package com.vet_saas.modules.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientPointsDashboardDto {
    private Integer totalPuntos;
    private List<PointHistoryDto> historialReciente;
}
