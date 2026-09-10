package com.vet_saas.modules.catalog.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.catalog.dto.CreateServiceDto;
import com.vet_saas.modules.catalog.dto.ServiceResponse;
import com.vet_saas.modules.catalog.dto.UpdateServiceDto;
import com.vet_saas.modules.catalog.model.Servicio;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PlanEnforcementService planEnforcementService;
    private final com.vet_saas.core.service.StorageService storageService;

    @Transactional
    public ServiceResponse createService(Usuario usuario, CreateServiceDto dto,
            org.springframework.web.multipart.MultipartFile imagen) {
        // Enforce service limits for EMPRESA and VETERINARIO
        if (isEmpresa(usuario)) {
            Empresa empresa = obtenerEmpresa(usuario);
            long currentCount = servicioRepository.countByEmpresaIdAndActivoTrue(empresa.getId());
            planEnforcementService.enforceServiceLimit(empresa.getId(), currentCount);
        } else if (isVeterinario(usuario)) {
            Veterinario veterinario = obtenerVeterinario(usuario);
            long currentCount = servicioRepository.countByVeterinarioIdAndActivoTrue(veterinario.getId());
            planEnforcementService.enforceServiceLimitForVeterinario(veterinario.getId(), currentCount);
        }

        String imagenUrl = null;
        if (imagen != null && !imagen.isEmpty()) {
            imagenUrl = storageService.uploadFile(imagen, "catalogo/servicios");
        }

        Servicio servicio = Servicio.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .precio(dto.precio())
                .duracionMinutos(dto.duracionMinutos() != null ? dto.duracionMinutos() : 30)
                .modalidad(dto.modalidad())
                .visible(dto.visible() != null ? dto.visible() : true)
                .activo(true)
                .imagenUrl(imagenUrl)
                .build();

        assignOwnership(usuario, servicio);

        return mapToResponse(servicioRepository.save(servicio));
    }

    @Transactional
    public ServiceResponse updateService(Usuario usuario, Long id, UpdateServiceDto dto,
            org.springframework.web.multipart.MultipartFile imagen) {
        Servicio servicio = getPropioServicio(usuario, id);

        if (dto.nombre() != null)
            servicio.setNombre(dto.nombre());
        if (dto.descripcion() != null)
            servicio.setDescripcion(dto.descripcion());
        if (dto.precio() != null)
            servicio.setPrecio(dto.precio());
        if (dto.duracionMinutos() != null)
            servicio.setDuracionMinutos(dto.duracionMinutos());
        if (dto.modalidad() != null)
            servicio.setModalidad(dto.modalidad());
        if (dto.visible() != null)
            servicio.setVisible(dto.visible());

        if (imagen != null && !imagen.isEmpty()) {
            // Delete old image if exists
            if (servicio.getImagenUrl() != null) {
                storageService.deleteFile(servicio.getImagenUrl());
            }
            servicio.setImagenUrl(storageService.uploadFile(imagen, "catalogo/servicios"));
        }

        return mapToResponse(servicioRepository.save(servicio));
    }

    @Transactional
    public void softDeleteService(Usuario usuario, Long id) {
        Servicio servicio = getPropioServicio(usuario, id);
        servicio.setActivo(false);
        servicio.setVisible(false);
        servicioRepository.save(servicio);
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getMyServices(Usuario usuario, Pageable pageable) {
        if (isEmpresa(usuario)) {
            Empresa empresa = obtenerEmpresa(usuario);
            return servicioRepository.findByEmpresaIdAndActivoTrue(empresa.getId(), pageable).map(this::mapToResponse);
        } else if (isVeterinario(usuario)) {
            Veterinario veterinario = obtenerVeterinario(usuario);
            return servicioRepository.findByVeterinarioIdAndActivoTrue(veterinario.getId(), pageable)
                    .map(this::mapToResponse);
        } else {
            throw new BusinessException("Usuario no autorizado para gestionar servicios.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getMarketplaceServices(String q, Long empresaId, Long veterinarioId,
            Pageable pageable) {
        return servicioRepository.findMarketplaceServices(q, empresaId, veterinarioId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        return servicioRepository.findByIdAndVisibleTrueAndActivoTrue(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio Marketplace", "id", id));
    }

    private Servicio getPropioServicio(Usuario usuario, Long servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", "id", servicioId));

        if (!servicio.getActivo()) {
            throw new ResourceNotFoundException("Servicio", "id", servicioId);
        }

        if (isEmpresa(usuario)) {
            Empresa empresa = obtenerEmpresa(usuario);
            if (!servicio.perteneceAEmpresa() || !servicio.getEmpresa().getId().equals(empresa.getId())) {
                throw new BusinessException("Acceso denegado: El servicio no pertenece a tu empresa.");
            }
        } else if (isVeterinario(usuario)) {
            Veterinario veterinario = obtenerVeterinario(usuario);
            if (!servicio.perteneceAVeterinario() || !servicio.getVeterinario().getId().equals(veterinario.getId())) {
                throw new BusinessException("Acceso denegado: El servicio no te pertenece.");
            }
        } else {
            throw new BusinessException("Rol no válido para modificar servicios.");
        }
        return servicio;
    }

    private void assignOwnership(Usuario usuario, Servicio servicio) {
        if (isEmpresa(usuario)) {
            servicio.setEmpresa(obtenerEmpresa(usuario));
        } else if (isVeterinario(usuario)) {
            servicio.setVeterinario(obtenerVeterinario(usuario));
        } else {
            throw new BusinessException("Debe ser perfil EMPRESA o VETERINARIO para crear servicios.");
        }
    }

    private boolean isEmpresa(Usuario usuario) {
        return "EMPRESA".equals(usuario.getRol().name());
    }

    private boolean isVeterinario(Usuario usuario) {
        return "VETERINARIO".equals(usuario.getRol().name());
    }

    private Empresa obtenerEmpresa(Usuario usuario) {
        return empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException(
                        "Debe registrar el perfil de su Empresa antes de operar con servicios."));
    }

    private Veterinario obtenerVeterinario(Usuario usuario) {
        return veterinarioRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException(
                        "Debe completar su perfil de Veterinario antes de ofrecer servicios."));
    }

    private ServiceResponse mapToResponse(Servicio servicio) {
        return new ServiceResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.getModalidad(),
                servicio.getActivo(),
                servicio.getVisible(),
                servicio.perteneceAEmpresa() ? servicio.getEmpresa().getId() : null,
                servicio.perteneceAEmpresa() ? servicio.getEmpresa().getNombreComercial() : null,
                servicio.perteneceAEmpresa() ? servicio.getEmpresa().getTipoServicio() : (servicio.perteneceAVeterinario() ? servicio.getVeterinario().getEspecialidad() : null),
                servicio.perteneceAVeterinario() ? servicio.getVeterinario().getId() : null,
                servicio.perteneceAVeterinario() ? servicio.getVeterinario().getNombres() : null,
                servicio.perteneceAVeterinario() ? servicio.getVeterinario().getApellidos() : null,
                servicio.getImagenUrl(),
                servicio.perteneceAEmpresa() ? servicio.getEmpresa().getMpPublicKey()
                        : (servicio.perteneceAVeterinario() ? servicio.getVeterinario().getMpPublicKey() : null),
                null,
                null);
    }
}
