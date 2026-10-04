package com.vet_saas.modules.appointment.service;

import com.vet_saas.modules.appointment.dto.HorarioAtencionRequest;
import com.vet_saas.modules.appointment.dto.HorarioAtencionResponse;
import com.vet_saas.modules.appointment.model.HorarioAtencion;
import com.vet_saas.modules.appointment.repository.HorarioAtencionRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioAtencionService {

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final EmpresaLookupService empresaLookupService;
    private final VeterinarioRepository veterinarioRepository;

    @Transactional(readOnly = true)
    public List<HorarioAtencionResponse> getHorariosByEmpresa(Usuario usuario) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        return horarioAtencionRepository.findByEmpresaIdOrderByDiaSemana(empresa.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public List<HorarioAtencionResponse> guardarHorarios(Usuario usuario, List<HorarioAtencionRequest> requests) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        validarRangos(requests);

        for (HorarioAtencionRequest request : requests) {
            DayOfWeek dia = request.getDiaSemana();
            HorarioAtencion horario = horarioAtencionRepository
                    .findByEmpresaIdAndDiaSemana(empresa.getId(), dia)
                    .orElseGet(() -> HorarioAtencion.builder()
                            .empresa(empresa)
                            .diaSemana(dia)
                            .build());

            horario.setHoraInicio(request.getHoraInicio());
            horario.setHoraFin(request.getHoraFin());
            horario.setCapacidad(request.getCapacidad());
            horario.setActivo(request.getActivo() == null || request.getActivo());

            horarioAtencionRepository.save(horario);
        }

        return getHorariosByEmpresa(usuario);
    }

    @Transactional(readOnly = true)
    public List<HorarioAtencionResponse> getHorariosByVeterinario(Usuario usuario) {
        Veterinario veterinario = getVeterinarioFromUsuario(usuario);
        return horarioAtencionRepository.findByVeterinarioIdOrderByDiaSemana(veterinario.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public List<HorarioAtencionResponse> guardarHorariosVeterinario(Usuario usuario, List<HorarioAtencionRequest> requests) {
        Veterinario veterinario = getVeterinarioFromUsuario(usuario);
        validarRangos(requests);

        for (HorarioAtencionRequest request : requests) {
            DayOfWeek dia = request.getDiaSemana();
            HorarioAtencion horario = horarioAtencionRepository
                    .findByVeterinarioIdAndDiaSemana(veterinario.getId(), dia)
                    .orElseGet(() -> HorarioAtencion.builder()
                            .veterinario(veterinario)
                            .diaSemana(dia)
                            .build());

            horario.setHoraInicio(request.getHoraInicio());
            horario.setHoraFin(request.getHoraFin());
            // Un veterinario atiende una cita a la vez
            horario.setCapacidad(1);
            horario.setActivo(request.getActivo() == null || request.getActivo());

            horarioAtencionRepository.save(horario);
        }

        return getHorariosByVeterinario(usuario);
    }

    private Veterinario getVeterinarioFromUsuario(Usuario usuario) {
        return veterinarioRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de veterinario no encontrado"));
    }

    // Un rango invertido o vacio (p. ej. 18:00 a 09:00) dejaba el dia sin ningun bloque reservable
    private void validarRangos(List<HorarioAtencionRequest> requests) {
        for (HorarioAtencionRequest request : requests) {
            boolean activo = request.getActivo() == null || request.getActivo();
            if (activo && !request.getHoraFin().isAfter(request.getHoraInicio())) {
                throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
            }
        }
    }

    private HorarioAtencionResponse mapToResponse(HorarioAtencion horario) {
        return new HorarioAtencionResponse(
                horario.getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getCapacidad(),
                horario.getActivo());
    }
}
