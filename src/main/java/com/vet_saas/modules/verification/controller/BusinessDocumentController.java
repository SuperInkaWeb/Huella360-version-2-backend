package com.vet_saas.modules.verification.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.verification.dto.BusinessDocumentResponse;
import com.vet_saas.modules.verification.service.BusinessDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Lado de la empresa: subir documentos de verificación (KYC) y ver su estado.
 */
@RestController
@RequestMapping("/api/v1/business/documents")
@RequiredArgsConstructor
public class BusinessDocumentController {

    private final BusinessDocumentService documentService;

    @PostMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<BusinessDocumentResponse>> subir(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("numeroDocumento") String numeroDocumento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                documentService.subirDocumento(usuario, file, tipoDocumento, numeroDocumento),
                "Documento subido correctamente"));
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<BusinessDocumentResponse>>> misDocumentos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getMisDocumentos(usuario),
                "Documentos recuperados"));
    }
}
