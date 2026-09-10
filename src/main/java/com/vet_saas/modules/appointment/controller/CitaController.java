package com.vet_saas.modules.appointment.controller;

import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.appointment.dto.CitaRequest;
import com.vet_saas.modules.appointment.dto.CitaResponse;
import com.vet_saas.modules.appointment.dto.CrearCitaEmpresaRequest;
import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.service.CitaService;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;

    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<LocalTime>>> getAvailableSlots(
            @RequestParam Long empresaId,
            @RequestParam Long servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.success(
                citaService.getAvailableSlots(empresaId, servicioId, fecha),
                "Horarios disponibles recuperados"));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<CitaResponse>> solicitarCita(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid CitaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                citaService.crearCita(usuario, request),
                "Cita solicitada exitosamente"));
    }

    @PostMapping("/empresa")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<CitaResponse>> crearCitaParaCliente(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid CrearCitaEmpresaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                citaService.crearCitaParaCliente(usuario, request),
                "Cita creada exitosamente"));
    }

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> getCitasEmpresa(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long empresaId) {
        if (usuario.getRol() != Role.ADMIN) {
            Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("No tienes acceso a esta empresa"));
            if (!empresa.getId().equals(empresaId)) {
                throw new ForbiddenException("No tienes acceso a las citas de esta empresa");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(
                citaService.getCitasByEmpresa(empresaId),
                "Citas de la empresa recuperadas"));
    }

    @GetMapping("/veterinario/{veterinarioId}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'EMPRESA', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> getCitasVeterinario(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long veterinarioId) {
        if (usuario.getRol() == Role.VETERINARIO) {
            Veterinario vet = veterinarioRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("Perfil de veterinario no encontrado"));
            if (!vet.getId().equals(veterinarioId)) {
                throw new ForbiddenException("No tienes acceso a las citas de otro veterinario");
            }
        } else if (usuario.getRol() == Role.EMPRESA) {
            Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("Empresa no encontrada"));
            veterinarioRepository.findById(veterinarioId)
                    .orElseThrow(() -> new ForbiddenException("Veterinario no encontrado"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                citaService.getCitasByVeterinario(veterinarioId),
                "Citas del veterinario recuperadas"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> getMisCitas(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
                citaService.getCitasByCliente(usuario.getId()),
                "Mis citas recuperadas"));
    }

    @PatchMapping("/{citaId}/status")
    @PreAuthorize("hasAnyRole('EMPRESA', 'VETERINARIO', 'ADMIN')")
    public ResponseEntity<ApiResponse<CitaResponse>> actualizarEstado(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long citaId,
            @RequestParam AppointmentStatus estado,
            @RequestParam(required = false) String notas) {
        if (usuario.getRol() != Role.ADMIN) {
            citaService.verifyOwnership(citaId, usuario);
        }
        return ResponseEntity.ok(ApiResponse.success(
                citaService.actualizarEstado(citaId, estado, notas),
                "Estado de la cita actualizado"));
    }
}
