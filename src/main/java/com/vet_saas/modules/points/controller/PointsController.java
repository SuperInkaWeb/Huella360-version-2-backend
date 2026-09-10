package com.vet_saas.modules.points.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.points.dto.ClientPointsDashboardDto;
import com.vet_saas.modules.points.dto.PointsConfigDto;
import com.vet_saas.modules.points.service.PointsConfigService;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final PointsConfigService configService;
    private final UsuarioService usuarioService;

    // --- CLIENT ENDPOINTS ---

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ClientPointsDashboardDto>> getMyPointsDashboard(Principal principal) {
        Long idPerfil = getUsuarioId(principal);
        ClientPointsDashboardDto dashboard = pointsService.getClientDashboard(idPerfil);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard de puntos obtenido"));
    }

    // --- ADMIN ENDPOINTS ---

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PointsConfigDto>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.success(configService.getAllConfigs(), "Configuración de puntos obtenida"));
    }

    @PutMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PointsConfigDto>> updateConfig(
            @PathVariable Long id,
            @RequestParam Integer puntosOtorgados,
            @RequestParam(required = false) Boolean activo) {
        PointsConfigDto response = configService.updateConfig(id, puntosOtorgados, activo);
        return ResponseEntity.ok(ApiResponse.success(response, "Configuración actualizada correctamente"));
    }

    private Long getUsuarioId(Principal principal) {
        Usuario usuario = usuarioService.findByCorreo(principal.getName());
        return usuario.getId();
    }
}
