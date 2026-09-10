package com.vet_saas.modules.leads.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.leads.dto.CreateLeadRequest;
import com.vet_saas.modules.leads.dto.LeadResponse;
import com.vet_saas.modules.leads.model.LeadEstado;
import com.vet_saas.modules.leads.service.LeadService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(
            @Valid @RequestBody CreateLeadRequest request) {
        LeadResponse response = leadService.createLead(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getMyLeads(
            @AuthenticationPrincipal Usuario usuario) {
        List<LeadResponse> response = leadService.getLeadsByEmpresa(usuario);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Long>> countMyLeads(
            @AuthenticationPrincipal Usuario usuario) {
        long count = leadService.countLeadsByEmpresa(usuario);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/{leadId}/status")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLeadStatus(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long leadId,
            @RequestParam LeadEstado estado) {
        LeadResponse response = leadService.updateLeadStatus(usuario, leadId, estado);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
