package com.vet_saas.modules.appointment.service;

import com.vet_saas.modules.appointment.dto.HorarioAtencionRequest;
import com.vet_saas.modules.appointment.dto.HorarioAtencionResponse;
import com.vet_saas.modules.appointment.model.HorarioAtencion;
import com.vet_saas.modules.appointment.repository.HorarioAtencionRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.user.model.Usuario;
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
