package com.vet_saas.modules.referral.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.referral.dto.ReferralCountResponse;
import com.vet_saas.modules.referral.service.ReferralService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/code")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<String>> getReferralCode(
            @AuthenticationPrincipal Usuario usuario) {
        String code = referralService.getOrGenerateReferralCode(usuario);
        return ResponseEntity.ok(ApiResponse.<String>success(code));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ReferralCountResponse>> getReferralCount(
            @AuthenticationPrincipal Usuario usuario) {
        ReferralCountResponse response = referralService.getReferralCount(usuario);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> applyReferralCode(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam String codigo) {
        referralService.applyReferralCode(usuario, codigo);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
