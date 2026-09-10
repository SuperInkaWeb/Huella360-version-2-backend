package com.vet_saas.modules.verification.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.verification.dto.BusinessDocumentResponse;
import com.vet_saas.modules.verification.model.BusinessDocument;
import com.vet_saas.modules.verification.model.DocumentStatus;
import com.vet_saas.modules.verification.repository.BusinessDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verificación de identidad (KYC) de la empresa: sube documentos (RUC, licencia)
 * como archivos; el admin los revisa antes de aprobar la empresa.
 */
@Service
@RequiredArgsConstructor
public class BusinessDocumentService {

    private final BusinessDocumentRepository documentRepository;
    private final EmpresaRepository empresaRepository;
    private final StorageService storageService;

    @Transactional
    public BusinessDocumentResponse subirDocumento(Usuario usuario, MultipartFile file,
                                                   String tipoDocumento, String numeroDocumento) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una empresa registrada"));

        String archivoUrl = storageService.uploadFile(file, "kyc");

        BusinessDocument documento = BusinessDocument.builder()
                .empresa(empresa)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .archivoUrl(archivoUrl)
                .build();

        return toResponse(documentRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<BusinessDocumentResponse> getMisDocumentos(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una empresa registrada"));
        return getDocumentosDeEmpresa(empresa.getId());
    }

    @Transactional(readOnly = true)
    public List<BusinessDocumentResponse> getDocumentosDeEmpresa(Long empresaId) {
        return documentRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BusinessDocumentResponse revisarDocumento(Long documentoId, DocumentStatus nuevoEstado) {
        if (nuevoEstado != DocumentStatus.VERIFIED && nuevoEstado != DocumentStatus.REJECTED) {
            throw new BusinessException("Estado inválido: use VERIFIED o REJECTED");
        }
        BusinessDocument documento = documentRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        documento.setEstado(nuevoEstado);
        documento.setRevisadoAt(LocalDateTime.now());
        return toResponse(documentRepository.save(documento));
    }

    private BusinessDocumentResponse toResponse(BusinessDocument d) {
        return BusinessDocumentResponse.builder()
                .id(d.getId())
                .tipoDocumento(d.getTipoDocumento())
                .numeroDocumento(d.getNumeroDocumento())
                .archivoUrl(d.getArchivoUrl())
                .estado(d.getEstado())
                .createdAt(d.getCreatedAt())
                .revisadoAt(d.getRevisadoAt())
                .build();
    }
}
