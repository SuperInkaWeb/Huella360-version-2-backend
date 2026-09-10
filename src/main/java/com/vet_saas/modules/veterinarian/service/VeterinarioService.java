package com.vet_saas.modules.veterinarian.service;

import com.vet_saas.core.utils.CryptoUtil;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.dto.VeterinarioRequest;
import com.vet_saas.modules.veterinarian.dto.VeterinarioResponse;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.vet_saas.modules.pet.dto.PetResponse;
import com.vet_saas.modules.pet.repository.MascotaRepository;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final CryptoUtil cryptoUtil;

    @Transactional
    public VeterinarioResponse createProfile(Long userId, VeterinarioRequest request) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (veterinarioRepository.findByUsuarioId(userId).isPresent()) {
            throw new BusinessException("El perfil de veterinario ya existe para este usuario");
        }

        if (veterinarioRepository.existsByNumeroColegiatura(request.numeroColegiatura())) {
            throw new BusinessException("El número de colegiatura ya está registrado");
        }

        Veterinario veterinario = Veterinario.builder()
                .usuario(usuario)
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .especialidad(request.especialidad())
                .numeroColegiatura(request.numeroColegiatura())
                .biografia(request.biografia())
                .aniosExperiencia(request.aniosExperiencia())
                .fotoPerfilUrl(request.fotoPerfilUrl())
                .mpAccessToken(request.mpAccessToken() != null ? cryptoUtil.encrypt(request.mpAccessToken()) : null)
                .mpPublicKey(request.mpPublicKey())
                .build();

        Veterinario saved = veterinarioRepository.save(veterinario);
        return mapToResponse(saved);
    }

    @Transactional
    public VeterinarioResponse updateProfile(Long userId, VeterinarioRequest request) {
        Veterinario veterinario = veterinarioRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new BusinessException("Perfil de veterinario no encontrado"));

        if (request.nombres() != null)
            veterinario.setNombres(request.nombres());
        if (request.apellidos() != null)
            veterinario.setApellidos(request.apellidos());
        if (request.especialidad() != null)
            veterinario.setEspecialidad(request.especialidad());
        if (request.biografia() != null)
            veterinario.setBiografia(request.biografia());
        if (request.aniosExperiencia() != null)
            veterinario.setAniosExperiencia(request.aniosExperiencia());
        if (request.fotoPerfilUrl() != null)
            veterinario.setFotoPerfilUrl(request.fotoPerfilUrl());

        if (request.numeroColegiatura() != null
                && !request.numeroColegiatura().equals(veterinario.getNumeroColegiatura())) {
            if (veterinarioRepository.existsByNumeroColegiatura(request.numeroColegiatura())) {
                throw new BusinessException("El número de colegiatura ya está registrado");
            }
            veterinario.setNumeroColegiatura(request.numeroColegiatura());
        }

        if (request.mpAccessToken() != null) {
            veterinario.setMpAccessToken(cryptoUtil.encrypt(request.mpAccessToken()));
        }
        if (request.mpPublicKey() != null) {
            veterinario.setMpPublicKey(request.mpPublicKey());
        }

        return mapToResponse(veterinarioRepository.save(veterinario));
    }

    @Transactional(readOnly = true)
    public VeterinarioResponse getProfileByUserId(Long userId) {
        Veterinario vet = veterinarioRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new BusinessException("Perfil de veterinario no encontrado"));
        return mapToResponse(vet);
    }

    @Transactional(readOnly = true)
    public List<VeterinarioResponse> getAllVerifiedVeterinarians() {
        return veterinarioRepository.findAll().stream()
                .filter(v -> v.getEstadoValidacion() == VerificationStatus.VERIFICADO)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetResponse> getPacientesByVeterinarioUsuarioId(Long userId) {
        Veterinario vet = veterinarioRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new BusinessException("Perfil de veterinario no encontrado"));

        return mascotaRepository.findPacientesByVeterinario(vet.getId()).stream()
                .map(this::mapToPetResponse)
                .toList();
    }

    private PetResponse mapToPetResponse(com.vet_saas.modules.pet.model.Mascota mascota) {
        return new PetResponse(
                mascota.getId(),
                mascota.getNombre(),
                mascota.getEspecie(),
                mascota.getRaza(),
                mascota.getSexo() != null ? mascota.getSexo() : null,
                mascota.getFechaNacimiento(),
                mascota.getPesoKg(),
                mascota.getFotoUrl(),
                mascota.getEsterilizado(),
                mascota.getObservacionesMedicas(),
                mascota.getCreatedAt());
    }

    private VeterinarioResponse mapToResponse(Veterinario vet) {
        return new VeterinarioResponse(
                vet.getId(),
                vet.getUsuario().getId(),
                vet.getNombres(),
                vet.getApellidos(),
                vet.getEspecialidad(),
                vet.getNumeroColegiatura(),
                vet.getBiografia(),
                vet.getAniosExperiencia(),
                vet.getFotoPerfilUrl(),
                vet.getEstadoValidacion(),
                vet.getUsuario().getCorreo(),
                vet.getMpPublicKey());
    }
}