package com.vet_saas.modules.adoption.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.adoption.dto.AdoptionResponse;
import com.vet_saas.modules.adoption.dto.ApplicationResponse;
import com.vet_saas.modules.adoption.dto.ApplyAdoptionDto;
import com.vet_saas.modules.adoption.dto.CreateAdoptionDto;
import com.vet_saas.modules.adoption.dto.RespondApplicationDto;
import com.vet_saas.modules.adoption.service.AdoptionService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/adoptions")
@RequiredArgsConstructor
public class AdoptionController {

        private final AdoptionService adoptionService;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<AdoptionResponse>> publishAdoption(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestBody @Valid CreateAdoptionDto dto) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.success(
                                                adoptionService.publishAdoption(usuario, dto),
                                                "Mascota publicada en adopción exitosamente"));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<Page<AdoptionResponse>>> getAvailableAdoptions(
                        Pageable pageable) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getAvailableAdoptions(pageable),
                                                "Adopciones recuperadas"));
        }

        @GetMapping("/public/company/{companyId}")
        public ResponseEntity<ApiResponse<Page<AdoptionResponse>>> getPublicAdoptionsByCompany(
                        @PathVariable Long companyId,
                        Pageable pageable) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getPublicAdoptionsByCompany(companyId, pageable),
                                                "Adopciones de la empresa recuperadas"));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<AdoptionResponse>> getAdoptionById(
                        @PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getAdoptionById(id),
                                                "Detalle de adopción recuperado"));
        }

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<AdoptionResponse>>> getMyAdoptions(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getMyAdoptions(usuario),
                                                "Tus publicaciones de adopción recuperadas"));
        }

        @GetMapping("/applications/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMySentApplications(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getMySentApplications(usuario),
                                                "Tus solicitudes de adopción enviadas recuperadas"));
        }

        @PostMapping("/{id}/apply")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> applyForAdoption(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable("id") Long id,
                        @RequestBody @Valid ApplyAdoptionDto dto) {
                adoptionService.applyForAdoption(usuario, id, dto);
                return ResponseEntity.ok(
                                ApiResponse.success(null, "Solicitud de adopción enviada al dueño de la mascota"));
        }

        @GetMapping("/{id}/applications")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplications(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                adoptionService.getApplicationsForMyAdoption(usuario, id),
                                                "Solicitudes recuperadas"));
        }

        @PatchMapping("/applications/{solicitudId}/response")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> respondToApplication(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long solicitudId,
                        @RequestBody @Valid RespondApplicationDto dto) {
                adoptionService.respondToApplication(usuario, solicitudId, dto);

                String message = dto.aprobar() != null && dto.aprobar()
                                ? "Solicitud aprobada y propiedad de mascota transferida con éxito"
                                : "Solicitud rechazada";

                return ResponseEntity.ok(
                                ApiResponse.success(null, message));
        }
}
