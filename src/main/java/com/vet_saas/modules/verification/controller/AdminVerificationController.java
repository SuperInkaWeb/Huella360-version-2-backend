package com.vet_saas.modules.verification.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.verification.dto.BusinessDocumentResponse;
import com.vet_saas.modules.verification.model.DocumentStatus;
import com.vet_saas.modules.verification.service.BusinessDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lado del admin: revisar los documentos de verificación (KYC) de una empresa.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final BusinessDocumentService documentService;

    @GetMapping("/empresas/{empresaId}/documentos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BusinessDocumentResponse>>> documentosDeEmpresa(
            @PathVariable Long empresaId) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentosDeEmpresa(empresaId),
                "Documentos de la empresa"));
    }

    @PutMapping("/documentos/{documentoId}/revisar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BusinessDocumentResponse>> revisar(
            @PathVariable Long documentoId,
            @RequestParam("estado") DocumentStatus estado) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.revisarDocumento(documentoId, estado),
                "Documento revisado"));
    }
}
