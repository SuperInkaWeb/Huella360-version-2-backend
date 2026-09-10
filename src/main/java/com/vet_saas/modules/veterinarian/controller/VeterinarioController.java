package com.vet_saas.modules.veterinarian.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.dto.VeterinarioRequest;
import com.vet_saas.modules.veterinarian.dto.VeterinarioResponse;
import com.vet_saas.modules.veterinarian.service.VeterinarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.vet_saas.modules.pet.dto.PetResponse;

@RestController
@RequestMapping("/api/v1/veterinarians")
@RequiredArgsConstructor
public class VeterinarioController {

        private final VeterinarioService veterinarioService;

        @PostMapping("/profile")
        @PreAuthorize("hasRole('VETERINARIO')")
        public ResponseEntity<ApiResponse<VeterinarioResponse>> createProfile(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestBody @Valid VeterinarioRequest request) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                veterinarioService.createProfile(usuario.getId(), request),
                                                "Perfil de veterinario creado correctamente"));
        }

        @GetMapping("/me")
        @PreAuthorize("hasRole('VETERINARIO')")
        public ResponseEntity<ApiResponse<VeterinarioResponse>> getMyProfile(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                veterinarioService.getProfileByUserId(usuario.getId()),
                                                "Perfil recuperado"));
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN')")
        public ResponseEntity<ApiResponse<java.util.List<VeterinarioResponse>>> getAllVerified() {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                veterinarioService.getAllVerifiedVeterinarians(),
                                                "Lista de facultativos recuperada"));
        }

        @GetMapping("/me/patients")
        @PreAuthorize("hasRole('VETERINARIO')")
        public ResponseEntity<ApiResponse<java.util.List<PetResponse>>> getMyPatients(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                veterinarioService.getPacientesByVeterinarioUsuarioId(usuario.getId()),
                                                "Pacientes recuperados"));
        }

        @PutMapping("/profile")
        @PreAuthorize("hasRole('VETERINARIO')")
        public ResponseEntity<ApiResponse<VeterinarioResponse>> updateProfile(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestBody @Valid VeterinarioRequest request) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                veterinarioService.updateProfile(usuario.getId(), request),
                                                "Perfil de veterinario actualizado correctamente"));
        }
}