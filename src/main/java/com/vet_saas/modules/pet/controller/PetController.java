package com.vet_saas.modules.pet.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.pet.dto.CreatePetDto;
import com.vet_saas.modules.pet.dto.PetResponse;
import com.vet_saas.modules.pet.dto.UpdatePetDto;
import com.vet_saas.modules.pet.service.HealthCardService;
import com.vet_saas.modules.pet.service.PetService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PetController {

        private final PetService petService;
        private final HealthCardService healthCardService;
        private final StorageService storageService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<PetResponse>> createPet(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestPart("data") @Valid CreatePetDto dto,
                        @RequestPart(value = "foto", required = false) MultipartFile foto) {
                String fotoUrl = null;
                if (foto != null && !foto.isEmpty()) {
                        fotoUrl = storageService.uploadFile(foto, "mascotas");
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.success(
                                                petService.createPet(usuario, dto, fotoUrl),
                                                "Mascota registrada exitosamente"));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<PetResponse>>> getMyPets(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                petService.getMyPets(usuario),
                                                "Lista de mascotas recuperadas"));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<PetResponse>> getPetById(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                petService.getPetById(usuario, id),
                                                "Mascota recuperada exitosamente"));
        }

        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<PetResponse>> updatePet(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long id,
                        @RequestPart(value = "data", required = false) @Valid UpdatePetDto dto,
                        @RequestPart(value = "foto", required = false) MultipartFile foto) {
                String fotoUrl = null;
                if (foto != null && !foto.isEmpty()) {
                        fotoUrl = storageService.uploadFile(foto, "mascotas");
                }

                UpdatePetDto safeDto = dto != null ? dto
                                : new UpdatePetDto(null, null, null, null, null, null, null, null);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                petService.updatePet(usuario, id, safeDto, fotoUrl),
                                                "Mascota actualizada exitosamente"));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deletePet(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long id) {
                petService.deletePet(usuario, id);
                return ResponseEntity.ok(
                                ApiResponse.success(null, "Mascota eliminada exitosamente"));
        }

        @GetMapping("/{id}/health-card")
        public ResponseEntity<byte[]> getHealthCard(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long id) {
                // Verificar que la mascota pertenece al usuario
                petService.getPetById(usuario, id);
                byte[] pdf = healthCardService.generateHealthCard(id);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header("Content-Disposition", "attachment; filename=carnet-salud-" + id + ".pdf")
                        .body(pdf);
        }
}
