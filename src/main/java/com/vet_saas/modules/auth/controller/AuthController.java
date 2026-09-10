package com.vet_saas.modules.auth.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.auth.dto.*;
import com.vet_saas.modules.auth.service.AuthService;
import com.vet_saas.modules.auth.service.RefreshTokenService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.register(request), "Usuario registrado exitosamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.login(request), "Inicio de sesión exitoso"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(refreshTokenService.refreshAccessToken(request.refreshToken()),
                        "Token renovado exitosamente"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        return ResponseEntity.ok(
                ApiResponse.successMessage("Sesión cerrada exitosamente"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal Usuario usuario) {
        refreshTokenService.revokeAllByUser(usuario.getId());
        return ResponseEntity.ok(
                ApiResponse.successMessage("Todas las sesiones han sido cerradas"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(usuario.getId(), request);
        return ResponseEntity.ok(
                ApiResponse.successMessage("Contraseña actualizada exitosamente"));
    }

    @org.springframework.web.bind.annotation.GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @org.springframework.web.bind.annotation.RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(
                ApiResponse.successMessage("Correo verificado exitosamente"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request.correo());
        return ResponseEntity.ok(
                ApiResponse.successMessage("Se ha enviado un correo para restablecer tu contraseña"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(
                ApiResponse.successMessage("Tu contraseña ha sido restablecida exitosamente"));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncAuth0Response>> syncAuth0User(@RequestBody @Valid SyncAuth0Request request) {
        var usuario = authService.syncAuth0User(request);
        SyncAuth0Response response = new SyncAuth0Response(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getRol()
        );
        return ResponseEntity.ok(
                ApiResponse.success(response, "Usuario sincronizado exitosamente"));
    }
}
