package com.vet_saas.modules.verification.dto;

import com.vet_saas.modules.verification.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDocumentResponse {
    private Long id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String archivoUrl;
    private DocumentStatus estado;
    private LocalDateTime createdAt;
    private LocalDateTime revisadoAt;
}
