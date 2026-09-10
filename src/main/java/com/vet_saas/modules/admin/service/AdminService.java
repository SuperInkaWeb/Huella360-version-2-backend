package com.vet_saas.modules.admin.service;

import com.vet_saas.modules.admin.dto.AdminCompanyResponseDto;
import com.vet_saas.modules.admin.dto.AdminStatsDto;
import com.vet_saas.modules.admin.dto.AdminUserResponseDto;
import com.vet_saas.modules.admin.dto.AdminVeterinarioResponseDto;
import com.vet_saas.modules.adoption.repository.AdopcionRepository;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.sales.model.EstadoOrden;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AdopcionRepository adoptionRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productRepository;
    private final OrdenRepository ordenRepository;

    @Transactional(readOnly = true)
    public AdminStatsDto getGlobalStats() {
        return AdminStatsDto.builder()
                .totalUsuarios(usuarioRepository.count())
                .totalEmpresas(empresaRepository.count())
                .totalVeterinarios(veterinarioRepository.count())
                .totalAdopciones(adoptionRepository.count())
                .totalServicios(servicioRepository.count())
                .totalProductos(productRepository.count())
                .totalOrdenes(ordenRepository.count())
                .ingresosGlobales(ordenRepository.sumTotalByEstado(EstadoOrden.PAGADO))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> getAllUsers(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));
        usuario.setEstado(!usuario.isEstado());
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Page<AdminCompanyResponseDto> getAllCompanies(Pageable pageable) {
        return empresaRepository.findAll(pageable)
                .map(this::mapToCompanyResponse);
    }

    @Transactional
    public void toggleCompanyStatus(Long companyId) {
        var empresa = empresaRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", companyId));

        // Use estadoValidacion to toggle
        if (empresa.getEstadoValidacion() == VerificationStatus.VERIFICADO) {
            empresa.setEstadoValidacion(VerificationStatus.RECHAZADO);
        } else {
            empresa.setEstadoValidacion(VerificationStatus.VERIFICADO);
        }

        empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public Page<AdminVeterinarioResponseDto> getAllVeterinarios(Pageable pageable) {
        return veterinarioRepository.findAll(pageable)
                .map(this::mapToVeterinarioResponse);
    }

    @Transactional
    public void toggleVeterinarioStatus(Long veterinarioId) {
        var veterinario = veterinarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", "id", veterinarioId));

        if (veterinario.getEstadoValidacion() == VerificationStatus.VERIFICADO) {
            veterinario.setEstadoValidacion(VerificationStatus.RECHAZADO);
        } else {
            veterinario.setEstadoValidacion(VerificationStatus.VERIFICADO);
        }

        veterinarioRepository.save(veterinario);
    }

    private AdminCompanyResponseDto mapToCompanyResponse(com.vet_saas.modules.company.model.Empresa e) {
        return AdminCompanyResponseDto.builder()
                .id(e.getId())
                .nombreComercial(e.getNombreComercial())
                .ruc(e.getRuc())
                .emailContacto(e.getEmailContacto())
                .tipoServicio(e.getTipoServicio())
                .telefonoContacto(e.getTelefonoContacto())
                .direccion(e.getDireccion())
                .ciudad(e.getCiudad())
                .pais(e.getPais())
                .estadoValidacion(e.getEstadoValidacion())
                .createdAt(e.getCreatedAt())
                .ownerEmail(e.getUsuarioPropietario() != null ? e.getUsuarioPropietario().getCorreo() : null)
                .build();
    }

    private AdminUserResponseDto mapToUserResponse(Usuario u) {
        return AdminUserResponseDto.builder()
                .id(u.getId())
                .correo(u.getCorreo())
                .rol(u.getRol())
                .estado(u.isEstado())
                .emailVerificado(u.isEmailVerificado())
                .createdAt(u.getCreatedAt())
                .nombre(u.getCorreo().split("@")[0]) // Temporary fallback, ideally join with profile
                .build();
    }

    private AdminVeterinarioResponseDto mapToVeterinarioResponse(
            com.vet_saas.modules.veterinarian.model.Veterinario v) {
        return AdminVeterinarioResponseDto.builder()
                .id(v.getId())
                .nombres(v.getNombres())
                .apellidos(v.getApellidos())
                .correo(v.getUsuario() != null ? v.getUsuario().getCorreo() : null)
                .especialidad(v.getEspecialidad())
                .numeroColegiatura(v.getNumeroColegiatura())
                .biografia(v.getBiografia())
                .aniosExperiencia(v.getAniosExperiencia())
                .fotoPerfilUrl(v.getFotoPerfilUrl())
                .estadoValidacion(v.getEstadoValidacion())
                .usuarioActivo(v.getUsuario() != null && v.getUsuario().isEstado())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
