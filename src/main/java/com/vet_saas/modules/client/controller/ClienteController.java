package com.vet_saas.modules.client.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.client.dto.ClienteCrmResponse;
import com.vet_saas.modules.client.dto.ClienteResponse;
import com.vet_saas.modules.client.dto.CreateClienteDto;
import com.vet_saas.modules.client.dto.UpdateClienteDto;
import com.vet_saas.modules.client.service.ClienteService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final com.vet_saas.core.service.StorageService storageService;

    // =========================================================================
    // SELF-SERVICE — rol CLIENTE
    // =========================================================================

    /**
     * POST /api/v1/clients/me
     * Crear perfil de cliente para el usuario logueado.
     */
    @PostMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ClienteResponse>> createPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid CreateClienteDto dto) {

        ClienteResponse response = clienteService.createPerfil(usuario, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Perfil de cliente creado exitosamente"));
    }

    /**
     * GET /api/v1/clients/me
     * Ver el perfil propio del cliente logueado.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ClienteResponse>> getMyPerfil(
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(ApiResponse.success(clienteService.getMyPerfil(usuario), "Perfil del cliente"));
    }

    /**
     * PUT /api/v1/clients/me
     * Actualizar perfil propio del cliente logueado (soporta multipart para foto).
     */
    @PutMapping(value = "/me", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ClienteResponse>> updateMyPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestPart(value = "data", required = false) @Valid UpdateClienteDto dto,
            @RequestPart(value = "foto", required = false) org.springframework.web.multipart.MultipartFile foto) {

        String fotoUrl = null;
        if (foto != null && !foto.isEmpty()) {
            fotoUrl = storageService.uploadFile(foto, "clientes");
        }

        UpdateClienteDto safeDto = dto != null ? dto : new UpdateClienteDto(null, null, null, null, null, null);

        return ResponseEntity
                .ok(ApiResponse.success(clienteService.updateMyPerfil(usuario, safeDto, fotoUrl),
                        "Perfil actualizado"));
    }

    /**
     * PATCH /api/v1/clients/me
     * Actualizar perfil propio del cliente logueado (legacy JSON version).
     */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<ClienteResponse>> updateMyPerfilLegacy(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid UpdateClienteDto dto) {

        return ResponseEntity
                .ok(ApiResponse.success(clienteService.updateMyPerfil(usuario, dto, null), "Perfil actualizado"));
    }

    /**
     * DELETE /api/v1/clients/me
     * Eliminar perfil propio del cliente logueado.
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> deleteMyPerfil(
            @AuthenticationPrincipal Usuario usuario) {

        clienteService.deleteMiPerfil(usuario);
        return ResponseEntity.ok(ApiResponse.success(null, "Perfil eliminado exitosamente"));
    }

    // =========================================================================
    // EMPRESA — ver clientes que han comprado en la empresa
    // =========================================================================

    /**
     * GET /api/v1/clients/empresa
     * Listar clientes de la empresa autenticada (los que tienen órdenes con ella).
     * Requiere rol EMPRESA.
     */
    @GetMapping("/empresa")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Page<ClienteResponse>>> getClientesByEmpresa(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, "updatedAt"));

        Page<ClienteResponse> result = clienteService.getClientesByEmpresa(usuario, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Clientes de la empresa"));
    }

    /**
     * GET /api/v1/clients/empresa/crm
     * Listar clientes de la empresa autenticada con estadisticas de CRM
     * (total gastado, total pedidos, total citas, ultima compra).
     * Requiere rol EMPRESA.
     */
    @GetMapping("/empresa/crm")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Page<ClienteCrmResponse>>> getClientesCrmByEmpresa(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, "updatedAt"));

        Page<ClienteCrmResponse> result = clienteService.getClientesCrmByEmpresa(usuario, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Clientes de la empresa con estadisticas CRM"));
    }

    /**
     * GET /api/v1/clients/empresa/{id}
     * Ver el perfil de un cliente específico que pertenece a la empresa.
     * Requiere rol EMPRESA.
     */
    @GetMapping("/empresa/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<ClienteResponse>> getClienteByIdForEmpresa(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(
                clienteService.getClienteByIdForEmpresa(usuario, id),
                "Perfil del cliente"));
    }

    // =========================================================================
    // ADMIN
    // =========================================================================

    /**
     * GET /api/v1/clients
     * Listar y buscar todos los clientes (Admin).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ClienteResponse>>> searchClientes(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ClienteResponse> result = clienteService.searchClientes(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Lista de clientes"));
    }

    /**
     * GET /api/v1/clients/{id}
     * Ver un cliente por id (Admin).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClienteResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.getClienteById(id), "Cliente encontrado"));
    }
}
