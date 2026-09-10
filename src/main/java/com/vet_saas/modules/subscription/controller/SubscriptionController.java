package com.vet_saas.modules.subscription.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.payment.dto.PaymentPreferenceResponse;
import com.vet_saas.modules.subscription.dto.PlanResponseDto;
import com.vet_saas.modules.subscription.dto.SuscripcionResponseDto;
import com.vet_saas.modules.subscription.dto.SubscriptionUsageDto;
import com.vet_saas.modules.subscription.model.Suscripcion;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

        private final SubscriptionService subscriptionService;

        @GetMapping("/plans")
        public ResponseEntity<ApiResponse<List<PlanResponseDto>>> getPlans(
                        @RequestParam(required = false) String tipo) {
                List<PlanResponseDto> plans = (tipo != null && !tipo.isBlank())
                                ? subscriptionService.getAvailablePlansByType(tipo)
                                : subscriptionService.getAvailablePlans();
                return ResponseEntity.ok(ApiResponse.success(plans,
                                "Planes de suscripción recuperados exitosamente"));
        }

        @GetMapping("/me")
        @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'CLIENTE')")
        public ResponseEntity<ApiResponse<SuscripcionResponseDto>> getMySubscription(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(ApiResponse.success(
                                subscriptionService.getSuscripcionActual(usuario),
                                "Suscripción actual recuperada exitosamente"));
        }

        @PatchMapping("/update-plan")
        @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'CLIENTE')")
        public ResponseEntity<ApiResponse<SuscripcionResponseDto>> updatePlan(
                        @AuthenticationPrincipal Usuario usuario,
                        @RequestParam Long planId) {
                return ResponseEntity.ok(ApiResponse.success(
                                subscriptionService.updatePlanForUsuario(usuario, planId),
                                "Plan de suscripción actualizado con éxito"));
        }

        @GetMapping("/usage/me")
        @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'CLIENTE')")
        public ResponseEntity<ApiResponse<SubscriptionUsageDto>> getUsage(@AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(ApiResponse.success(
                                subscriptionService.getUsageMetrics(usuario),
                                "Métricas de uso recuperadas exitosamente"));
        }

        @PostMapping("/checkout/{planId}")
        @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'CLIENTE')")
        public ResponseEntity<ApiResponse<PaymentPreferenceResponse>> createCheckout(
                        @AuthenticationPrincipal Usuario usuario,
                        @PathVariable Long planId) {
                return ResponseEntity.ok(ApiResponse.success(
                                subscriptionService.createSubscriptionCheckout(usuario, planId),
                                "Preferencia de pago generada exitosamente"));
        }

        @PatchMapping("/cancel")
        @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'CLIENTE')")
        public ResponseEntity<ApiResponse<SuscripcionResponseDto>> cancelSubscription(
                        @AuthenticationPrincipal Usuario usuario) {
                return ResponseEntity.ok(ApiResponse.success(
                                subscriptionService.cancelSubscription(usuario),
                                "Suscripción cancelada exitosamente"));
        }

}
