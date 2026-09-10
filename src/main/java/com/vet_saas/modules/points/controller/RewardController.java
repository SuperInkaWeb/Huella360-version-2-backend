package com.vet_saas.modules.points.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.points.dto.CreateRewardDto;
import com.vet_saas.modules.points.dto.RedeemedRewardDto;
import com.vet_saas.modules.points.dto.RewardDto;
import com.vet_saas.modules.points.service.RewardService;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.service.UsuarioService;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final UsuarioService usuarioService;
    private final EmpresaRepository empresaRepository;

    // --- COMPANY ENDPOINTS ---

    @PostMapping("/company")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<RewardDto>> createReward(@Valid @RequestBody CreateRewardDto dto, Principal principal) {
        Long idEmpresa = getEmpresaId(principal);
        RewardDto response = rewardService.createReward(idEmpresa, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Recompensa creada exitosamente"));
    }

    @GetMapping("/company")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Page<RewardDto>>> getMyCompanyRewards(Pageable pageable, Principal principal) {
        Long idEmpresa = getEmpresaId(principal);
        Page<RewardDto> result = rewardService.getAllRewardsByEmpresa(idEmpresa, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Recompensas de la empresa"));
    }

    @PutMapping("/company/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<RewardDto>> updateReward(@PathVariable Long id, @Valid @RequestBody CreateRewardDto dto, Principal principal) {
        Long idEmpresa = getEmpresaId(principal);
        RewardDto response = rewardService.updateReward(id, idEmpresa, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Recompensa actualizada exitosamente"));
    }

    @DeleteMapping("/company/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Void>> deactivateReward(@PathVariable Long id, Principal principal) {
        Long idEmpresa = getEmpresaId(principal);
        rewardService.deactivateReward(id, idEmpresa);
        return ResponseEntity.ok(ApiResponse.success(null, "Recompensa desactivada"));
    }

    // --- CLIENT ENDPOINTS ---

    @GetMapping("/business/{idEmpresa}")
    public ResponseEntity<ApiResponse<Page<RewardDto>>> getActiveRewardsForCompany(@PathVariable Long idEmpresa, Pageable pageable) {
        Page<RewardDto> result = rewardService.getRewardsByEmpresa(idEmpresa, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Recompensas disponibles"));
    }

    @PostMapping("/{idRecompensa}/redeem")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<RedeemedRewardDto>> redeemReward(@PathVariable Long idRecompensa, Principal principal) {
        Long idPerfil = getUsuarioId(principal);
        RedeemedRewardDto response = rewardService.redeemReward(idPerfil, idRecompensa);
        return ResponseEntity.ok(ApiResponse.success(response, "¡Recompensa canjeada con éxito!"));
    }

    @GetMapping("/my-redeemed")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<Page<RedeemedRewardDto>>> getMyRedeemedRewards(Pageable pageable, Principal principal) {
        Long idPerfil = getUsuarioId(principal);
        Page<RedeemedRewardDto> result = rewardService.getMyRedeemedRewards(idPerfil, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Mis recompensas canjeadas"));
    }
    
    @GetMapping("/checkout/available/{idEmpresa}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<List<RedeemedRewardDto>>> getAvailableRewardsForCheckout(@PathVariable Long idEmpresa, Principal principal) {
        Long idPerfil = getUsuarioId(principal);
        List<RedeemedRewardDto> result = rewardService.getAvailableRewardsForCheckout(idPerfil, idEmpresa);
        return ResponseEntity.ok(ApiResponse.success(result, "Recompensas disponibles para el checkout"));
    }

    private Long getUsuarioId(Principal principal) {
        Usuario usuario = usuarioService.findByCorreo(principal.getName());
        return usuario.getId();
    }

    private Long getEmpresaId(Principal principal) {
        Usuario usuario = usuarioService.findByCorreo(principal.getName());
        return empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "usuarioId", usuario.getId()))
                .getId();
    }
}
