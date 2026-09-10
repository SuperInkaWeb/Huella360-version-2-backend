package com.vet_saas.modules.medical_record.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.medical_record.dto.CreateHistoriaClinicaDto;
import com.vet_saas.modules.medical_record.dto.HistoriaClinicaResponse;
import com.vet_saas.modules.medical_record.model.HistoriaClinica;
import com.vet_saas.modules.medical_record.repository.HistoriaClinicaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public HistoriaClinicaResponse createEntry(Long userId, CreateHistoriaClinicaDto dto) {
        Veterinario vet = veterinarioRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new BusinessException("Solo los veterinarios pueden registrar historias clínicas"));

        Mascota mascota = mascotaRepository.findById(dto.mascotaId())
                .orElseThrow(() -> new BusinessException("Mascota no encontrada"));

        Cita cita = null;
        if (dto.citaId() != null) {
            cita = citaRepository.findById(dto.citaId())
                    .orElseThrow(() -> new BusinessException("Cita no encontrada"));
        }

        HistoriaClinica entry = HistoriaClinica.builder()
                .mascota(mascota)
                .veterinario(vet)
                .cita(cita)
                .diagnostico(dto.diagnostico())
                .tratamiento(dto.tratamiento())
                .notas(dto.notas())
                .pesoKg(dto.pesoKg())
                .fechaRegistro(dto.fechaRegistro() != null ? dto.fechaRegistro() : LocalDateTime.now())
                .build();

        return mapToResponse(historiaClinicaRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<HistoriaClinicaResponse> getHistoryByMascota(Long mascotaId) {
        return historiaClinicaRepository.findByMascotaIdOrderByFechaRegistroDesc(mascotaId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private HistoriaClinicaResponse mapToResponse(HistoriaClinica h) {
        return new HistoriaClinicaResponse(
                h.getId(),
                h.getMascota().getId(),
                h.getVeterinario().getId(),
                h.getVeterinario().getNombres() + " " + h.getVeterinario().getApellidos(),
                h.getCita() != null ? h.getCita().getId() : null,
                h.getDiagnostico(),
                h.getTratamiento(),
                h.getNotas(),
                h.getPesoKg(),
                h.getFechaRegistro(),
                h.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public boolean hasVetAccessToPet(Long vetId, Long mascotaId) {
        return citaRepository.existsByVeterinarioIdAndMascotaId(vetId, mascotaId);
    }

    @Transactional(readOnly = true)
    public boolean hasEmpresaAccessToPet(Long usuarioId, Long mascotaId) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuarioId).orElse(null);
        if (empresa == null) return false;
        return mascotaRepository.findById(mascotaId)
                .map(m -> m.getUsuario() != null && m.getUsuario().getId().equals(usuarioId))
                .orElse(false);
    }
}
