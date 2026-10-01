package com.vet_saas.modules.admin.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.admin.dto.AdminCompanyResponseDto;
import com.vet_saas.modules.admin.dto.AdminStatsDto;
import com.vet_saas.modules.admin.dto.AdminUserResponseDto;
import com.vet_saas.modules.admin.dto.AdminVeterinarioResponseDto;
import com.vet_saas.modules.admin.service.AdminService;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final EmpresaRepository empresaRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getGlobalStats() {
        return ResponseEntity
                .ok(ApiResponse.success(adminService.getGlobalStats(), "Estadísticas globales recuperadas"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponseDto>>> getAllUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(q, pageable), "Usuarios recuperados"));
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id,
            @AuthenticationPrincipal Usuario admin) {
        adminService.toggleUserStatus(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Estatus del usuario actualizado"));
    }

    @PatchMapping("/companies/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleCompanyStatus(@PathVariable Long id) {
        adminService.toggleCompanyStatus(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Estatus de la empresa actualizado"));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<Page<AdminCompanyResponseDto>>> getAllCompanies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VerificationStatus estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllCompanies(q, estado, pageable), "Empresas recuperadas"));
    }

    @GetMapping("/veterinarios")
    public ResponseEntity<ApiResponse<Page<AdminVeterinarioResponseDto>>> getAllVeterinarios(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VerificationStatus estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity
                .ok(ApiResponse.success(adminService.getAllVeterinarios(q, estado, pageable), "Veterinarios recuperados"));
    }

    @PatchMapping("/veterinarios/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleVeterinarioStatus(@PathVariable Long id) {
        adminService.toggleVeterinarioStatus(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Estatus del veterinario actualizado"));
    }
}
