package com.vet_saas.modules.support.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.support.dto.CreateTicketRequest;
import com.vet_saas.modules.support.dto.TicketResponse;
import com.vet_saas.modules.support.service.SupportTicketService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<TicketResponse>> crearTicket(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                supportTicketService.crearTicket(usuario, request),
                "Ticket creado exitosamente"));
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMisTickets(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
                supportTicketService.getMisTickets(usuario),
                "Tickets recuperados"));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.success(
                supportTicketService.getTicket(usuario, publicId),
                "Ticket recuperado"));
    }
}
