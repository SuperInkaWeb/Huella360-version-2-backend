package com.vet_saas.modules.geo.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.geo.dto.LugarCercanoResponse;
import com.vet_saas.modules.geo.service.CercaniaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Búsqueda pública por cercanía (no requiere sesión: cubierta por /api/v1/public/**). */
@RestController
@RequestMapping("/api/v1/public/cercanos")
@RequiredArgsConstructor
public class CercaniaController {

    private final CercaniaService cercaniaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LugarCercanoResponse>>> buscarCercanos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radioKm,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String tipoServicio,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(ApiResponse.success(
                cercaniaService.buscar(lat, lng, radioKm, tipo, tipoServicio, limite),
                "Lugares cercanos recuperados"));
    }
}
