package com.vet_saas.modules.user.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.user.dto.UpdateRoleRequest;
import com.vet_saas.modules.user.dto.UserMeResponse;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.user.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(@AuthenticationPrincipal Usuario usuario) {
        UserMeResponse response = new UserMeResponse(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getRol()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Información del usuario"));
    }

    @GetMapping("/exists/{correo}")
    public ResponseEntity<ApiResponse<Boolean>> existsByCorreo(@PathVariable("correo") String correo) {
        boolean exists = usuarioRepository.findByCorreo(correo).isPresent();
        return ResponseEntity.ok(ApiResponse.success(exists, "Verificación completada"));
    }

    @PatchMapping("/me/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserMeResponse>> updateMyRole(
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        Role nuevoRol = Role.valueOf(request.rol().toUpperCase());
        UserMeResponse response = usuarioService.updateMyRole(usuario, nuevoRol);

        return ResponseEntity.ok(ApiResponse.success(response, "Rol actualizado correctamente a " + nuevoRol.name()));
    }
}
