package com.vet_saas.modules.company.staff.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.company.staff.dto.AddStaffDto;
import com.vet_saas.modules.company.staff.dto.StaffResponse;
import com.vet_saas.modules.company.staff.service.StaffService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPRESA')")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getMyStaff(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        staffService.getMyStaff(usuario),
                        "Lista de staff recuperada"
                )
        );
    }

    @DeleteMapping("/{veterinarioId}")
    public ResponseEntity<ApiResponse<Void>> removeStaff(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long veterinarioId
    ) {
        staffService.removeVeterinarian(usuario, veterinarioId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Veterinario removido del staff")
        );
    }


    @PostMapping("/invite")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<StaffResponse>> inviteStaff(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid AddStaffDto dto
    ) {
        StaffResponse response = staffService.inviteVeterinarian(usuario, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invitación enviada al veterinario"));
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getMyInvitations(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.getMyInvitations(usuario),
                "Invitaciones recuperadas"
        ));
    }

    @PutMapping("/invitations/{staffId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long staffId
    ) {
        staffService.respondToInvitation(usuario, staffId, true);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitación aceptada. ¡Ahora eres parte del equipo!"));
    }

    @PutMapping("/invitations/{staffId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long staffId
    ) {
        staffService.respondToInvitation(usuario, staffId, false);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitación rechazada"));
    }
}