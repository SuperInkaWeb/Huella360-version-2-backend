package com.vet_saas.modules.leads.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.leads.dto.CreateLeadRequest;
import com.vet_saas.modules.leads.dto.LeadResponse;
import com.vet_saas.modules.leads.model.Lead;
import com.vet_saas.modules.leads.model.LeadEstado;
import com.vet_saas.modules.leads.repository.LeadRepository;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.empresaId()));

        Lead lead = Lead.builder()
                .empresa(empresa)
                .clienteNombre(request.clienteNombre())
                .clienteEmail(request.clienteEmail())
                .clienteTelefono(request.clienteTelefono())
                .servicioSolicitado(request.servicioSolicitado())
                .mensaje(request.mensaje())
                .estado(LeadEstado.NUEVO)
                .build();

        Lead saved = leadRepository.save(lead);
        log.info("Lead {} creado para empresa {}", saved.getId(), empresa.getId());

        return LeadResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeadsByEmpresa(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "propietarioId", usuario.getId()));

        return leadRepository.findByEmpresaIdOrderByCreatedAtDesc(empresa.getId())
                .stream()
                .map(LeadResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countLeadsByEmpresa(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "propietarioId", usuario.getId()));
        return leadRepository.countByEmpresaId(empresa.getId());
    }

    @Transactional
    public LeadResponse updateLeadStatus(Usuario usuario, Long leadId, LeadEstado nuevoEstado) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));

        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "propietarioId", usuario.getId()));

        if (!lead.getEmpresa().getId().equals(empresa.getId())) {
            throw new com.vet_saas.core.exceptions.types.BusinessException("No tienes permiso para modificar este lead");
        }

        lead.setEstado(nuevoEstado);
        Lead saved = leadRepository.save(lead);

        return LeadResponse.fromEntity(saved);
    }
}
