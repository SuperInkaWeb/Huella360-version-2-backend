package com.vet_saas.modules.company.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.company.dto.CompanyResponse;
import com.vet_saas.modules.company.dto.CreateCompanyDto;
import com.vet_saas.modules.company.dto.UpdateCompanyDto;
import com.vet_saas.modules.company.dto.UpdateMercadoPagoCredentialsDto;
import com.vet_saas.modules.company.service.CompanyService;
import com.vet_saas.modules.pet.dto.PetResponse;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CompanyController {

        private final CompanyService companyService;
        private final StorageService storageService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<CompanyResponse>> createProfile(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestPart("data") @Valid CreateCompanyDto dto,
                        @RequestPart(value = "logo", required = false) MultipartFile logo,
                        @RequestPart(value = "banner", required = false) MultipartFile banner) {
                String logoUrl = null;
                String bannerUrl = null;

                if (logo != null && !logo.isEmpty())
                        logoUrl = storageService.uploadFile(logo, "logos");
                if (banner != null && !banner.isEmpty())
                        bannerUrl = storageService.uploadFile(banner, "banners");

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.createProfile(usuario, dto, logoUrl, bannerUrl),
                                                "Perfil de empresa creado exitosamente"));
        }

        @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<CompanyResponse>> updateProfile(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestPart(value = "data", required = false) @Valid UpdateCompanyDto dto,
                        @RequestPart(value = "logo", required = false) MultipartFile logo,
                        @RequestPart(value = "banner", required = false) MultipartFile banner) {
                String logoUrl = null;
                String bannerUrl = null;

                if (logo != null && !logo.isEmpty()) {
                        logoUrl = storageService.uploadFile(logo, "logos");
                }

                if (banner != null && !banner.isEmpty()) {
                        bannerUrl = storageService.uploadFile(banner, "banners");
                }

                UpdateCompanyDto safeDto = dto != null ? dto
                                : new UpdateCompanyDto(null, null, null, null, null, null, null, null, null);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.updateProfile(usuario, safeDto, logoUrl, bannerUrl),
                                                "Perfil de empresa actualizado exitosamente"));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<CompanyResponse>> getMyCompany(@AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.getProfile(usuario),
                                                "Perfil de empresa recuperado exitosamente"));
        }

        @GetMapping("/public")
        @PreAuthorize("permitAll()")
        public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CompanyResponse>>> getAllPublicCompanies(
                        org.springframework.data.domain.Pageable pageable) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.getAllPublicCompanies(pageable),
                                                "Empresas públicas recuperadas exitosamente"));
        }

        @GetMapping("/public/{id}")
        @PreAuthorize("permitAll()")
        public ResponseEntity<ApiResponse<CompanyResponse>> getPublicCompany(@PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.getPublicProfile(id),
                                                "Perfil público de empresa recuperado exitosamente"));
        }

        @GetMapping("/me/patients")
        @PreAuthorize("hasRole('EMPRESA')")
        public ResponseEntity<ApiResponse<java.util.List<PetResponse>>> getMyPatients(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(
                                ApiResponse.success(
                                                companyService.getPatientsByCompany(usuario),
                                                "Pacientes de la empresa recuperados exitosamente"));
        }

        @PatchMapping("/mercadopago")
        @PreAuthorize("hasRole('EMPRESA')")
        public ResponseEntity<ApiResponse<Void>> updateMercadoPagoCredentials(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestBody @Valid UpdateMercadoPagoCredentialsDto dto) {

                companyService.updateMercadoPagoCredentials(usuario, dto.getMpAccessToken(), dto.getMpPublicKey());

                return ResponseEntity.ok(
                                ApiResponse.success(null,
                                                "Credenciales de Mercado Pago actualizadas y cifradas exitosamente"));
        }
}