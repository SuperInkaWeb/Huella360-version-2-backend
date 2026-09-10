package com.vet_saas.modules.delivery.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.delivery.dto.request.RepartidorRequestDTO;
import com.vet_saas.modules.delivery.dto.request.UbicacionDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.RepartidorResponseDTO;
import com.vet_saas.modules.delivery.service.RepartidorService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repartidores")
@RequiredArgsConstructor
@Slf4j
public class RepartidorController {

    private final RepartidorService repartidorService;
    private final StorageService storageService;

    /** POST /api/v1/repartidores/me — Registrar perfil */
    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<RepartidorResponseDTO>> createPerfil(
            @AuthenticationPrincipal Usuario principal,
            @RequestPart("data") @Valid RepartidorRequestDTO dto,
            @RequestPart(value = "fotoPerfil", required = false) MultipartFile foto) {

        String fotoUrl = null;
        if (foto != null && !foto.isEmpty()) {
            log.info("Recibida foto de perfil para repartidor: {} ({} bytes)", foto.getOriginalFilename(), foto.getSize());
            fotoUrl = storageService.uploadFile(foto, "repartidores");
        } else {
            log.info("No se recibió foto de perfil o está vacía para el repartidor");
        }

        return ResponseEntity.ok(ApiResponse.success(
                repartidorService.createProfile(principal.getId(), dto, fotoUrl),
                "Perfil de repartidor procesado exitosamente"));
    }

    /** PUT /api/v1/repartidores/me — Actualizar perfil y foto */
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<RepartidorResponseDTO>> updatePerfil(
            @AuthenticationPrincipal Usuario principal,
            @RequestPart(value = "data", required = false) @Valid RepartidorRequestDTO dto,
            @RequestPart(value = "fotoPerfil", required = false) MultipartFile foto) {

        String fotoUrl = null;
        if (foto != null && !foto.isEmpty()) {
            log.info("Recibida nueva foto de perfil para actualización de repartidor: {} ({} bytes)", foto.getOriginalFilename(), foto.getSize());
            fotoUrl = storageService.uploadFile(foto, "repartidores");
        }

        RepartidorRequestDTO safeDto = dto != null ? dto : new RepartidorRequestDTO();
        return ResponseEntity.ok(ApiResponse.success(
                repartidorService.updateProfile(principal.getId(), safeDto, fotoUrl),
                "Perfil actualizado correctamente"));
    }

    /** Perfil del repartidor autenticado */
    @GetMapping("/me")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<RepartidorResponseDTO>> getMiPerfil(
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
                repartidorService.getByUsuarioId(principal.getId()),
                "Perfil del repartidor recuperado"));
    }

    /** El repartidor activa o desactiva su disponibilidad */
    @PatchMapping("/me/disponibilidad")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> cambiarDisponibilidad(
            @RequestParam boolean disponible,
            @AuthenticationPrincipal Usuario principal) {
        repartidorService.cambiarDisponibilidad(principal.getId(), disponible);
        return ResponseEntity.ok(ApiResponse.success(null, "Disponibilidad actualizada"));
    }

    /**
     * Actualizar ubicacion GPS via HTTP (complementa el WebSocket).
     * Usa esto como fallback si el WS se desconecta.
     */
    @PatchMapping("/me/ubicacion")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> actualizarUbicacion(
            @Valid @RequestBody UbicacionDTO dto,
            @AuthenticationPrincipal Usuario principal) {
        repartidorService.actualizarUbicacion(principal.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Ubicación actualizada"));
    }

    /** Deliveries activos del repartidor (hasta maxPedidosSimultaneos) */
    @GetMapping("/me/deliveries-activos")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<List<DeliveryResponseDTO>>> getDeliveriesActivos(
            @AuthenticationPrincipal Usuario principal) {
        List<DeliveryResponseDTO> activos = repartidorService.getDeliveriesActivos(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(activos, "Deliveries activos recuperados"));
    }

    /** Historial de entregas del repartidor */
    @GetMapping("/me/historial")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<List<DeliveryResponseDTO>>> getHistorial(
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
                repartidorService.getHistorial(principal.getId()),
                "Historial recuperado"));
    }
}
