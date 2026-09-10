package com.vet_saas.modules.company.staff.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.company.staff.dto.AddStaffDto;
import com.vet_saas.modules.company.staff.dto.StaffResponse;
import com.vet_saas.modules.company.staff.model.StaffStatus;
import com.vet_saas.modules.company.staff.model.StaffVeterinario;
import com.vet_saas.modules.company.staff.repository.StaffRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public StaffResponse inviteVeterinarian(Usuario usuarioPropietario, AddStaffDto dto) {
        validarRolEmpresa(usuarioPropietario);

        Empresa empresa = obtenerEmpresaDelPropietario(usuarioPropietario);
        Usuario usuarioVet = obtenerUsuarioPorEmail(dto.emailVeterinario());
        Veterinario veterinario = obtenerVeterinarioDelUsuario(usuarioVet);

        Optional<StaffVeterinario> staffExistente = staffRepository
                .findByEmpresaIdAndVeterinarioId(empresa.getId(), veterinario.getId());

        if (staffExistente.isPresent()) {
            return manejarReInvitacion(staffExistente.get(), dto.rolInterno());
        }

        StaffVeterinario nuevoStaff = StaffVeterinario.builder()
                .empresa(empresa)
                .veterinario(veterinario)
                .rolInterno(dto.rolInterno())
                .estado(StaffStatus.PENDIENTE)
                .build();

        return mapToResponse(staffRepository.save(nuevoStaff));
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getMyStaff(Usuario usuarioPropietario) {
        validarRolEmpresa(usuarioPropietario);
        Empresa empresa = obtenerEmpresaDelPropietario(usuarioPropietario);

        return staffRepository
                .findByEmpresaIdAndEstadoWithVeterinario(empresa.getId(), StaffStatus.ACTIVO)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void respondToInvitation(Usuario usuarioVet, Long staffId, boolean aceptar) {
        Veterinario vet = veterinarioRepository.findByUsuarioId(usuarioVet.getId())
                .orElseThrow(() -> new BusinessException("No eres veterinario"));

        StaffVeterinario solicitud = staffRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));

        if (!solicitud.getVeterinario().getId().equals(vet.getId())) {
            throw new BusinessException("No tienes permiso para responder esta solicitud");
        }

        if (aceptar) {
            solicitud.setEstado(StaffStatus.ACTIVO);
        } else {
            solicitud.setEstado(StaffStatus.RECHAZADO);
        }
        staffRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getMyInvitations(Usuario usuarioVet) {
        Veterinario vet = veterinarioRepository.findByUsuarioId(usuarioVet.getId())
                .orElseThrow(() -> new BusinessException("Perfil no encontrado"));

        return staffRepository.findByVeterinarioIdAndEstadoWithVeterinario(vet.getId(), StaffStatus.PENDIENTE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void removeVeterinarian(Usuario usuarioPropietario, Long idVeterinario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuarioPropietario.getId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        StaffVeterinario staff = staffRepository.findByEmpresaIdAndVeterinarioId(empresa.getId(), idVeterinario)
                .orElseThrow(() -> new BusinessException("El veterinario no pertenece a tu staff"));

        staff.setEstado(StaffStatus.FINALIZADO);
        staffRepository.save(staff);
    }

    // HELPERS

    private void validarRolEmpresa(Usuario usuario) {
        if (usuario.getRol() != Role.EMPRESA) {
            throw new BusinessException("Solo empresas pueden realizar esta acción");
        }
    }

    private Empresa obtenerEmpresaDelPropietario(Usuario usuario) {
        return empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No tienes una empresa registrada"));
    }

    private Veterinario obtenerVeterinarioDelUsuario(Usuario usuario) {
        return veterinarioRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No tienes perfil de veterinario"));
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByCorreo(email)
                .orElseThrow(() -> new BusinessException("No existe usuario con ese correo"));
    }

    private StaffResponse manejarReInvitacion(StaffVeterinario staff, String nuevoRol) {
        if (staff.getEstado() == StaffStatus.ACTIVO || staff.getEstado() == StaffStatus.PENDIENTE) {
            throw new BusinessException("Ya existe una solicitud activa con este veterinario");
        }

        staff.setEstado(StaffStatus.PENDIENTE);
        staff.setRolInterno(nuevoRol);
        return mapToResponse(staffRepository.save(staff));
    }

    private StaffResponse mapToResponse(StaffVeterinario staff) {
        Veterinario vet = staff.getVeterinario();
        return new StaffResponse(
                staff.getId(),
                vet.getId(),
                vet.getNombres(),
                vet.getApellidos(),
                vet.getEspecialidad(),
                vet.getFotoPerfilUrl(),
                staff.getRolInterno(),
                StaffStatus.ACTIVO.equals(staff.getEstado())
        );
    }

}