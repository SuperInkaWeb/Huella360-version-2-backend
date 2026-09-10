package com.vet_saas.modules.reminder.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.reminder.dto.CreateRecordatorioRequest;
import com.vet_saas.modules.reminder.dto.RecordatorioResponse;
import com.vet_saas.modules.reminder.service.RecordatorioService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recordatorios")
@RequiredArgsConstructor
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecordatorioResponse>> createRecordatorio(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CreateRecordatorioRequest request) {
        RecordatorioResponse response = recordatorioService.createRecordatorio(usuario, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecordatorioResponse>>> getMyRecordatorios(
            @AuthenticationPrincipal Usuario usuario) {
        List<RecordatorioResponse> response = recordatorioService.getMyRecordatorios(usuario);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{recordatorioId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecordatorio(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long recordatorioId) {
        recordatorioService.deleteRecordatorio(usuario, recordatorioId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
