package com.vet_saas.modules.pet.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.pet.dto.CreatePetDto;
import com.vet_saas.modules.pet.dto.PetResponse;
import com.vet_saas.modules.pet.dto.UpdatePetDto;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final MascotaRepository mascotaRepository;
    private final EmpresaRepository empresaRepository;
    private final SubscriptionService subscriptionService;
    private final PlanEnforcementService planEnforcementService;
    private final PointsService pointsService;
    private final ClienteRepository clienteRepository;

    @Transactional
    public PetResponse createPet(Usuario usuario, CreatePetDto dto, String fotoUrl) {
        // Validar límite de mascotas para empresas
        if (usuario.getRol() == Role.EMPRESA) {
            Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                    .orElseThrow(() -> new BusinessException("No se encontró el perfil de empresa para este usuario"));

            long currentCount = mascotaRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
            if (!subscriptionService.canAddMascota(empresa.getId(), currentCount)) {
                throw new BusinessException(
                        "Has alcanzado el límite de mascotas permitido por tu plan actual. Mejora tu suscripción para agregar más.");
            }
        }

        // Validar límite de mascotas para clientes (B2C)
        if (usuario.getRol() == Role.CLIENTE) {
            long currentCount = mascotaRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
            planEnforcementService.enforceMascotaLimit(usuario.getId(), currentCount, "CLIENTE");
        }

        Mascota mascota = Mascota.builder()
                .usuario(usuario)
                .nombre(dto.nombre())
                .especie(dto.especie())
                .raza(dto.raza())
                .sexo(dto.sexo())
                .fechaNacimiento(dto.fechaNacimiento())
                .pesoKg(dto.pesoKg())
                .fotoUrl(fotoUrl)
                .esterilizado(dto.esterilizado() != null ? dto.esterilizado() : false)
                .observacionesMedicas(dto.observacionesMedicas())
                .activo(true)
                .build();

        Mascota saved = mascotaRepository.save(mascota);
        
        // Grant logic for First Pet (Gamification)
        if (usuario.getRol() == Role.CLIENTE) {
            try {
                // If the user only has 1 active pet, it means this is their first
                long currentCount = mascotaRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
                if (currentCount == 1) {
                    clienteRepository.findByUsuarioId(usuario.getId()).ifPresent(perfil -> {
                        pointsService.addPoints(perfil.getId(), "PRIMERA_MASCOTA", saved.getId(), "Bono por registrar tu primera mascota: " + saved.getNombre());
                    });
                }
            } catch (Exception e) {
                 log.error("Error granting first pet points: {}", e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PetResponse> getMyPets(Usuario usuario) {
        return mascotaRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PetResponse getPetById(Usuario usuario, Long petId) {
        Mascota mascota = mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(petId, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", "id", petId));

        return mapToResponse(mascota);
    }

    @Transactional
    public PetResponse updatePet(Usuario usuario, Long petId, UpdatePetDto dto, String fotoUrl) {
        Mascota mascota = mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(petId, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", "id", petId));

        if (dto.nombre() != null && !dto.nombre().isBlank())
            mascota.setNombre(dto.nombre());
        if (dto.especie() != null && !dto.especie().isBlank())
            mascota.setEspecie(dto.especie());
        if (dto.raza() != null)
            mascota.setRaza(dto.raza());
        if (dto.sexo() != null)
            mascota.setSexo(dto.sexo());
        if (dto.fechaNacimiento() != null)
            mascota.setFechaNacimiento(dto.fechaNacimiento());
        if (dto.pesoKg() != null)
            mascota.setPesoKg(dto.pesoKg());
        if (dto.esterilizado() != null)
            mascota.setEsterilizado(dto.esterilizado());
        if (dto.observacionesMedicas() != null)
            mascota.setObservacionesMedicas(dto.observacionesMedicas());

        if (fotoUrl != null && !fotoUrl.isBlank()) {
            mascota.setFotoUrl(fotoUrl);
        }

        Mascota updated = mascotaRepository.save(mascota);
        return mapToResponse(updated);
    }

    @Transactional
    public void deletePet(Usuario usuario, Long petId) {
        Mascota mascota = mascotaRepository.findByIdAndUsuarioIdAndActivoTrue(petId, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", "id", petId));

        mascota.setActivo(false);
        mascotaRepository.save(mascota);
    }

    private PetResponse mapToResponse(Mascota mascota) {
        return new PetResponse(
                mascota.getId(),
                mascota.getNombre(),
                mascota.getEspecie(),
                mascota.getRaza(),
                mascota.getSexo(),
                mascota.getFechaNacimiento(),
                mascota.getPesoKg(),
                mascota.getFotoUrl(),
                mascota.getEsterilizado(),
                mascota.getObservacionesMedicas(),
                mascota.getCreatedAt());
    }
}
