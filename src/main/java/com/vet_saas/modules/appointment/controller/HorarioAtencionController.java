package com.vet_saas.modules.appointment.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.appointment.dto.HorarioAtencionRequest;
import com.vet_saas.modules.appointment.dto.HorarioAtencionResponse;
import com.vet_saas.modules.appointment.service.HorarioAtencionService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies/me/horarios")
@RequiredArgsConstructor
public class HorarioAtencionController {

    private final HorarioAtencionService horarioAtencionService;

    @GetMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<HorarioAtencionResponse>>> getMisHorarios(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
                horarioAtencionService.getHorariosByEmpresa(usuario),
                "Horarios de atención recuperados"));
    }

    @PutMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<HorarioAtencionResponse>>> actualizarHorarios(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid List<HorarioAtencionRequest> horarios) {
        return ResponseEntity.ok(ApiResponse.success(
                horarioAtencionService.guardarHorarios(usuario, horarios),
                "Horarios de atención actualizados"));
    }
}
