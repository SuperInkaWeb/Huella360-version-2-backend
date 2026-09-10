package com.vet_saas.modules.complaint.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.complaint.dto.ReclamoRequestDto;
import com.vet_saas.modules.complaint.dto.ReclamoResponse;
import com.vet_saas.modules.complaint.model.EstadoReclamo;
import com.vet_saas.modules.complaint.service.ReclamoService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reclamos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReclamoController {

    private final ReclamoService reclamoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> registrarReclamo(
            @AuthenticationPrincipal Usuario usuario,
            @RequestPart("reclamo") @Valid ReclamoRequestDto reclamoDto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {

        String pdfUrl = reclamoService.registrarReclamo(usuario, reclamoDto, archivo);

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", pdfUrl), "Reclamo registrado exitosamente"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReclamoResponse>> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoReclamo estado,
            @RequestParam(required = false) String notas) {

        return ResponseEntity.ok(ApiResponse.success(
                ReclamoResponse.fromEntity(reclamoService.actualizarEstado(id, estado, notas)),
                "Estado del reclamo actualizado"));
    }
}
