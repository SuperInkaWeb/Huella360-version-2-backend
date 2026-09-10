package com.vet_saas.modules.adoption.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.adoption.dto.*;
import com.vet_saas.modules.adoption.model.Adopcion;
import com.vet_saas.modules.adoption.model.EstadoAdopcion;
import com.vet_saas.modules.adoption.model.EstadoSolicitud;
import com.vet_saas.modules.adoption.model.SolicitudAdopcion;
import com.vet_saas.modules.adoption.repository.AdopcionRepository;
import com.vet_saas.modules.adoption.repository.SolicitudAdopcionRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.client.repository.ClienteRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionService {

    private final AdopcionRepository adopcionRepository;
    private final SolicitudAdopcionRepository solicitudRepository;
    private final MascotaRepository mascotaRepository;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PointsService pointsService;
    private final ClienteRepository clienteRepository;

    @Transactional
    public AdoptionResponse publishAdoption(Usuario usuario, CreateAdoptionDto dto) {
        Mascota mascota = mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(dto.mascotaId(), usuario.getId())
                .orElseThrow(() -> new BusinessException("La mascota no existe o no te pertenece."));

        // Verificar si la mascota ya está en adopción y no ha sido cerrada
        boolean yaEnAdopcion = adopcionRepository.existsByMascotaIdAndEstadoInAndActivoTrue(
                mascota.getId(),
                List.of(EstadoAdopcion.DISPONIBLE, EstadoAdopcion.PAUSADO));

        if (yaEnAdopcion) {
            throw new BusinessException("Esta mascota ya se encuentra publicada en adopción.");
        }

        Adopcion adopcion = Adopcion.builder()
                .mascota(mascota)
                .publicadoPor(usuario)
                .titulo(dto.titulo())
                .historia(dto.historia())
                .requisitos(dto.requisitos())
                .ubicacionCiudad(dto.ubicacionCiudad())
                .estado(EstadoAdopcion.DISPONIBLE)
                .activo(true)
                .build();

        Adopcion saved = adopcionRepository.save(adopcion);
        return mapToAdoptionResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdoptionResponse> getAvailableAdoptions(Pageable pageable) {
        return adopcionRepository.findByEstadoAndActivoTrue(EstadoAdopcion.DISPONIBLE, pageable)
                .map(this::mapToAdoptionResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdoptionResponse> getPublicAdoptionsByCompany(Long companyId, Pageable pageable) {
        Empresa empresa = empresaRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", companyId));

        return adopcionRepository.findByPublicadoPorIdAndEstadoAndActivoTrue(
                empresa.getUsuarioPropietario().getId(),
                EstadoAdopcion.DISPONIBLE,
                pageable)
                .map(this::mapToAdoptionResponse);
    }

    @Transactional(readOnly = true)
    public AdoptionResponse getAdoptionById(Long id) {
        return adopcionRepository.findByIdAndActivoTrue(id)
                .map(this::mapToAdoptionResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Adopción", "id", id));
    }

    @Transactional(readOnly = true)
    public List<AdoptionResponse> getMyAdoptions(Usuario usuario) {
        return adopcionRepository.findByPublicadoPorIdAndActivoTrue(usuario.getId()).stream()
                .map(this::mapToAdoptionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void applyForAdoption(Usuario usuario, Long adopcionId, ApplyAdoptionDto dto) {
        Adopcion adopcion = adopcionRepository.findByIdAndActivoTrue(adopcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Adopción", "id", adopcionId));

        if (!adopcion.getEstado().equals(EstadoAdopcion.DISPONIBLE)) {
            throw new BusinessException("Esta mascota ya no se encuentra disponible para adopción.");
        }

        if (adopcion.getPublicadoPor().getId().equals(usuario.getId())) {
            throw new BusinessException("No puedes aplicar para adoptar tu propia mascota.");
        }

        if (solicitudRepository.existsByAdopcionIdAndInteresadoId(adopcionId, usuario.getId())) {
            throw new BusinessException("Ya has enviado una solicitud para esta adopción.");
        }

        SolicitudAdopcion solicitud = SolicitudAdopcion.builder()
                .adopcion(adopcion)
                .interesado(usuario)
                .mensajePresentacion(dto.mensajePresentacion())
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        solicitudRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForMyAdoption(Usuario usuario, Long adopcionId) {
        Adopcion adopcion = adopcionRepository.findByIdAndActivoTrue(adopcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Adopción", "id", adopcionId));

        if (!adopcion.getPublicadoPor().getId().equals(usuario.getId())) {
            throw new BusinessException("No tienes permiso para ver las solicitudes de esta adopción.");
        }

        return solicitudRepository.findByAdopcionId(adopcionId).stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMySentApplications(Usuario usuario) {
        return solicitudRepository.findByInteresadoIdOrderByFechaSolicitudDesc(usuario.getId())
                .stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void respondToApplication(Usuario usuario, Long solicitudId, RespondApplicationDto dto) {
        try {
            SolicitudAdopcion solicitud = solicitudRepository.findById(solicitudId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", solicitudId));

            Adopcion adopcion = solicitud.getAdopcion();

            if (!adopcion.getPublicadoPor().getId().equals(usuario.getId())) {
                throw new BusinessException("No tienes permiso para responder a esta solicitud.");
            }

            if (!adopcion.getEstado().equals(EstadoAdopcion.DISPONIBLE)) {
                throw new BusinessException("Esta adopción ya no se encuentra disponible.");
            }

            if (!solicitud.getEstado().equals(EstadoSolicitud.PENDIENTE)) {
                throw new BusinessException("Esta solicitud ya fue procesada anteriormente.");
            }

            solicitud.setFechaRespuesta(LocalDateTime.now());

            if (dto.aprobar()) {
                // Flujo atómico de aprobación y transferencia de propiedad
                solicitud.setEstado(EstadoSolicitud.APROBADA);

                // Cerrar la adopción
                adopcion.setEstado(EstadoAdopcion.ADOPTADO);
                adopcionRepository.save(adopcion);

                // Rechazar otras solicitudes concurrentes en PENDIENTE
                solicitudRepository.rejectOtherApplications(adopcion.getId(), solicitud.getId());

                // Transferencia de mascota
                Mascota mascota = adopcion.getMascota();
                mascota.setUsuario(solicitud.getInteresado());
                mascotaRepository.save(mascota);
                
                // Puntos por adopción
                if (solicitud.getInteresado().getRol() == com.vet_saas.modules.user.model.Role.CLIENTE) {
                     try {
                         clienteRepository.findByUsuarioId(solicitud.getInteresado().getId()).ifPresent(perfil -> {
                              pointsService.addPoints(perfil.getId(), "ADOPCION", adopcion.getId(), "Bono por darle un hogar a " + mascota.getNombre());
                         });
                     } catch(Exception e) {
                         log.error("Error rewarding adoption points: {}", e.getMessage());
                     }
                }

            } else {
                // Simplemente rechazar esta solicitud particular
                solicitud.setEstado(EstadoSolicitud.RECHAZADA);
                solicitud.setMotivoRechazo(dto.motivoRechazo());
            }

            solicitudRepository.save(solicitud);

        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException(
                    "No se pudo procesar la solicitud porque la adopción acaba de ser modificada por otro proceso. Por favor, intenta de nuevo.");
        }
    }

    private AdoptionResponse mapToAdoptionResponse(Adopcion adopcion) {
        return new AdoptionResponse(
                adopcion.getId(),
                adopcion.getMascota().getId(),
                adopcion.getMascota().getNombre(),
                adopcion.getMascota().getFotoUrl(),
                adopcion.getTitulo(),
                adopcion.getHistoria(),
                adopcion.getRequisitos(),
                adopcion.getUbicacionCiudad(),
                adopcion.getEstado(),
                adopcion.getPublicadoPor().getId(),
                adopcion.getPublicadoPor().getCorreo(),
                getTipoServicioPublicador(adopcion.getPublicadoPor().getId()),
                adopcion.getFechaPublicacion());
    }

    private String getTipoServicioPublicador(Long usuarioId) {
        return empresaRepository.findByUsuarioPropietarioId(usuarioId)
                .map(Empresa::getTipoServicio)
                .orElseGet(() -> veterinarioRepository.findByUsuarioId(usuarioId)
                        .map(Veterinario::getEspecialidad).orElse(null));
    }

    private ApplicationResponse mapToApplicationResponse(SolicitudAdopcion sol) {
        return new ApplicationResponse(
                sol.getId(),
                sol.getAdopcion().getId(),
                sol.getInteresado().getId(),
                sol.getInteresado().getCorreo(),
                sol.getMensajePresentacion(),
                sol.getEstado(),
                sol.getMotivoRechazo(),
                sol.getFechaSolicitud(),
                sol.getFechaRespuesta());
    }
}
