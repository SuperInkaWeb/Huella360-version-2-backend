package com.vet_saas.modules.dashboard.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.dashboard.dto.ActividadRecienteDto;
import com.vet_saas.modules.dashboard.dto.DashboardMetricsDto;
import com.vet_saas.modules.dashboard.dto.VentaDiariaDto;
import com.vet_saas.modules.dashboard.service.DashboardService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getDashboardMetrics(
            @AuthenticationPrincipal Usuario usuario) {

        Long empresaId = dashboardService.resolveEmpresaId(usuario.getId());
        DashboardMetricsDto metrics = dashboardService.getMetrics(empresaId);

        return ResponseEntity.ok(
                ApiResponse.success(metrics, "Métricas del dashboard recuperadas exitosamente"));
    }

    @GetMapping("/chart-data")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<VentaDiariaDto>>> getChartData(
            @AuthenticationPrincipal Usuario usuario) {

        Long empresaId = dashboardService.resolveEmpresaId(usuario.getId());
        List<VentaDiariaDto> chartData = dashboardService.getChartData(empresaId);

        return ResponseEntity.ok(
                ApiResponse.success(chartData, "Datos del gráfico recuperados exitosamente"));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<ActividadRecienteDto>>> getRecentActivity(
            @AuthenticationPrincipal Usuario usuario) {

        Long empresaId = dashboardService.resolveEmpresaId(usuario.getId());
        List<ActividadRecienteDto> activity = dashboardService.getRecentActivity(empresaId);

        return ResponseEntity.ok(
                ApiResponse.success(activity, "Actividad reciente recuperada exitosamente"));
    }
}
