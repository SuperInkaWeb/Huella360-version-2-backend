package com.vet_saas.modules.ia.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.ia.dto.HealthAlertRequest;
import com.vet_saas.modules.ia.dto.HealthAlertResponse;
import com.vet_saas.modules.ia.service.IaService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class IaController {

    private final IaService iaService;

    @PostMapping("/health-alerts")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<HealthAlertResponse>> generateHealthAlerts(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody HealthAlertRequest request) {
        HealthAlertResponse response = iaService.generateHealthAlerts(usuario, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
