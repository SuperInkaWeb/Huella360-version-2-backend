package com.vet_saas.modules.complaint.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.complaint.dto.ReclamoRequestDto;
import com.vet_saas.modules.complaint.dto.ReclamoResponse;
import com.vet_saas.modules.complaint.model.EstadoReclamo;
import com.vet_saas.modules.complaint.service.ReclamoService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reclamos")
@RequiredArgsConstructor
public class ReclamoController {

    private final ReclamoService reclamoService;

    /**
     * Libro de Reclamaciones: publico (sin cuenta), como exige la normativa de proteccion al consumidor.
     * Antes la clase entera exigia autenticacion y el frontend no enviaba token: respondia 401 siempre.
     * Si hay sesion, el reclamo queda asociado al usuario; si no, usuario == null.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Map<String, String>>> registrarReclamo(
            @AuthenticationPrincipal Usuario usuario,
            @RequestPart("reclamo") @Valid ReclamoRequestDto reclamoDto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {

        ReclamoService.ResultadoReclamo r = reclamoService.registrarReclamo(usuario, reclamoDto, archivo);

        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(r.id()));
        data.put("numero", r.numero());
        data.put("url", r.pdfUrl()); // puede ser null si fallo la generacion del PDF
        return ResponseEntity.ok(ApiResponse.success(data, "Reclamo N° " + r.numero() + " registrado exitosamente"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReclamoResponse>>> listar(
            @RequestParam(required = false) EstadoReclamo estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pagina = reclamoService.listar(estado, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(ReclamoResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(pagina, "Reclamos recuperados"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReclamoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ReclamoResponse.fromEntity(reclamoService.obtener(id)), "Reclamo recuperado"));
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
