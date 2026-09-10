package com.vet_saas.modules.medical_record.controller;

import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.medical_record.dto.CreateHistoriaClinicaDto;
import com.vet_saas.modules.medical_record.dto.HistoriaClinicaResponse;
import com.vet_saas.modules.medical_record.service.HistoriaClinicaService;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService medicalRecordService;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> createEntry(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid CreateHistoriaClinicaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        medicalRecordService.createEntry(usuario.getId(), dto),
                        "Entrada de historia clínica registrada exitosamente"));
    }

    @GetMapping("/pet/{mascotaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<HistoriaClinicaResponse>>> getByPet(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long mascotaId) {
        if (usuario.getRol() != Role.ADMIN) {
            verifyPetAccess(usuario, mascotaId);
        }
        return ResponseEntity.ok(
                ApiResponse.success(
                        medicalRecordService.getHistoryByMascota(mascotaId),
                        "Historia clínica recuperada"));
    }

    private void verifyPetAccess(Usuario usuario, Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElse(null);
        if (mascota == null) {
            return;
        }

        if (usuario.getRol() == Role.CLIENTE) {
            if (mascota.getUsuario() == null || !mascota.getUsuario().getId().equals(usuario.getId())) {
                throw new ForbiddenException("No tienes acceso a la historia clínica de esta mascota");
            }
        } else if (usuario.getRol() == Role.VETERINARIO) {
            Veterinario vet = veterinarioRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (vet == null) {
                throw new ForbiddenException("Perfil de veterinario no encontrado");
            }
            boolean hasAccess = medicalRecordService.hasVetAccessToPet(vet.getId(), mascotaId);
            if (!hasAccess) {
                throw new ForbiddenException("No tienes acceso a la historia clínica de esta mascota");
            }
        } else if (usuario.getRol() == Role.EMPRESA) {
            boolean hasAccess = medicalRecordService.hasEmpresaAccessToPet(usuario.getId(), mascotaId);
            if (!hasAccess) {
                throw new ForbiddenException("No tienes acceso a la historia clínica de esta mascota");
            }
        }
    }
}
