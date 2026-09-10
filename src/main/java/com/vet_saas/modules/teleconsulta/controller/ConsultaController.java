package com.vet_saas.modules.teleconsulta.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.teleconsulta.dto.ChatMensajeResponse;
import com.vet_saas.modules.teleconsulta.dto.ConsultaResponse;
import com.vet_saas.modules.teleconsulta.dto.SendMessageRequest;
import com.vet_saas.modules.teleconsulta.service.ConsultaService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teleconsultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> createConsulta(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam Long veterinarioId,
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) String mensajeInicial) {
        ConsultaResponse response = consultaService.createConsulta(usuario, veterinarioId, mascotaId, mensajeInicial);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsultaResponse>>> getMyConsultas(
            @AuthenticationPrincipal Usuario usuario) {
        List<ConsultaResponse> response = consultaService.getMyConsultas(usuario);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{consultaId}/mensajes")
    public ResponseEntity<ApiResponse<List<ChatMensajeResponse>>> getMensajes(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId) {
        List<ChatMensajeResponse> response = consultaService.getMensajes(usuario, consultaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{consultaId}/mensajes")
    public ResponseEntity<ApiResponse<ChatMensajeResponse>> sendMessage(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMensajeResponse response = consultaService.sendMessage(usuario, consultaId, request.contenido(), request.tipo());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{consultaId}/accept")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> acceptConsulta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId) {
        ConsultaResponse response = consultaService.acceptConsulta(usuario, consultaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{consultaId}/start")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> startConsulta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId) {
        ConsultaResponse response = consultaService.startConsulta(usuario, consultaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{consultaId}/finish")
    public ResponseEntity<ApiResponse<ConsultaResponse>> finishConsulta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId) {
        ConsultaResponse response = consultaService.finishConsulta(usuario, consultaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{consultaId}/cancel")
    public ResponseEntity<ApiResponse<ConsultaResponse>> cancelConsulta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long consultaId) {
        ConsultaResponse response = consultaService.cancelConsulta(usuario, consultaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
