package com.vet_saas.modules.company.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.company.dto.MercadoPagoConnectDto;
import com.vet_saas.modules.company.service.CompanyService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final CompanyService companyService;

    @PostMapping("/mercadopago/exchange")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Void>> exchangeCode(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid MercadoPagoConnectDto dto) {

        companyService.connectMercadoPago(usuario, dto.getCode(), dto.getRedirectUri());
        return ResponseEntity.ok(ApiResponse.success(null, "Conexión con Mercado Pago exitosa"));
    }
}
