package com.vet_saas.modules.appointment.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.appointment.dto.CitaRequest;
import com.vet_saas.modules.appointment.dto.CitaResponse;
import com.vet_saas.modules.appointment.dto.CrearCitaEmpresaRequest;
import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.appointment.model.HorarioAtencion;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.appointment.repository.HorarioAtencionRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.catalog.model.Servicio;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final ServicioRepository servicioRepository;
    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final HorarioAtencionRepository horarioAtencionRepository;

    @Transactional
    public CitaResponse crearCita(Usuario cliente, CitaRequest request) {
        Mascota mascota = null;
        if (request.getMascotaId() != null) {
            mascota = mascotaRepository.findById(request.getMascotaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        }

        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Veterinario veterinario = null;
        if (request.getVeterinarioId() != null) {
            veterinario = veterinarioRepository.findById(request.getVeterinarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));
        }

        LocalTime horaFin = request.getHoraInicio().plusMinutes(servicio.getDuracionMinutos());

        validarCupoDisponible(empresa, request.getFechaProgramada(), request.getHoraInicio(), horaFin);

        if (veterinario != null) {
            boolean ocupado = citaRepository.existsOverlap(
                    veterinario.getId(), request.getFechaProgramada(),
                    request.getHoraInicio(), horaFin);
            if (ocupado) {
                throw new BusinessException("El veterinario no está disponible en ese horario. Seleccione otro horario.");
            }
        }

        Cita cita = Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .servicio(servicio)
                .empresa(empresa)
                .veterinario(veterinario)
                .fechaProgramada(request.getFechaProgramada())
                .horaInicio(request.getHoraInicio())
                .horaFin(horaFin)
                .estado(AppointmentStatus.SOLICITADA)
                .notasCliente(request.getNotasCliente())
                .build();

        return CitaResponse.fromEntity(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse crearCitaParaCliente(Usuario empresaUsuario, CrearCitaEmpresaRequest request) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(empresaUsuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        String guestEmail = null;
        String guestNombre = null;
        String guestTelefono = null;
        Usuario cliente;

        if (request.getClienteId() != null) {
            cliente = usuarioRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        } else if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()) {
            guestEmail = request.getGuestEmail().trim().toLowerCase();
            guestNombre = request.getGuestNombre() != null ? request.getGuestNombre().trim() : null;
            guestTelefono = request.getGuestTelefono() != null ? request.getGuestTelefono().trim() : null;
            final String finalGuestEmail = guestEmail;

            cliente = usuarioRepository.findByCorreo(finalGuestEmail).orElseGet(() -> {
                Usuario nuevo = Usuario.builder()
                        .correo(finalGuestEmail)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .rol(Role.CLIENTE)
                        .estado(true)
                        .emailVerificado(false)
                        .build();
                return usuarioRepository.save(nuevo);
            });
        } else {
            throw new BusinessException("Se requiere un cliente registrado o datos de un invitado (email)");
        }

        Mascota mascota = null;
        if (request.getMascotaId() != null) {
            mascota = mascotaRepository.findById(request.getMascotaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        }

        Veterinario veterinario = null;
        if (request.getVeterinarioId() != null) {
            veterinario = veterinarioRepository.findById(request.getVeterinarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));
        }

        LocalTime horaFin = request.getHoraInicio().plusMinutes(servicio.getDuracionMinutos());

        validarCupoDisponible(empresa, request.getFechaProgramada(), request.getHoraInicio(), horaFin);

        if (veterinario != null) {
            boolean ocupado = citaRepository.existsOverlap(
                    veterinario.getId(), request.getFechaProgramada(),
                    request.getHoraInicio(), horaFin);
            if (ocupado) {
                throw new BusinessException("El veterinario no está disponible en ese horario.");
            }
        }

        Cita cita = Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .servicio(servicio)
                .empresa(empresa)
                .veterinario(veterinario)
                .fechaProgramada(request.getFechaProgramada())
                .horaInicio(request.getHoraInicio())
                .horaFin(horaFin)
                .estado(AppointmentStatus.SOLICITADA)
                .notasCliente(request.getNotasCliente())
                .notasInternas(request.getNotasInternas())
                .guestNombre(guestNombre)
                .guestEmail(guestEmail)
                .guestTelefono(guestTelefono)
                .build();

        return CitaResponse.fromEntity(citaRepository.save(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> getCitasByEmpresa(Long empresaId) {
        // Fix: Repository had typo in findByEmpresaId return type
        return ((List<Cita>) (Object) citaRepository.findByEmpresaId(empresaId)).stream()
                .map(CitaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> getCitasByVeterinario(Long veterinarioId) {
        return citaRepository.findByVeterinarioId(veterinarioId).stream()
                .map(CitaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> getCitasByCliente(Long clienteId) {
        return citaRepository.findByClienteId(clienteId).stream()
                .map(CitaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CitaResponse actualizarEstado(Long citaId, AppointmentStatus nuevoEstado, String notasInternas) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        cita.setEstado(nuevoEstado);
        if (notasInternas != null) {
            cita.setNotasInternas(notasInternas);
        }

        return CitaResponse.fromEntity(citaRepository.save(cita));
    }

    @Transactional(readOnly = true)
    public void verifyOwnership(Long citaId, Usuario usuario) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        if (usuario.getRol() == Role.EMPRESA) {
            Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("Empresa no encontrada"));
            if (cita.getEmpresa() == null || !cita.getEmpresa().getId().equals(empresa.getId())) {
                throw new ForbiddenException("No tienes acceso a esta cita");
            }
        } else if (usuario.getRol() == Role.VETERINARIO) {
            Veterinario vet = veterinarioRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("Perfil de veterinario no encontrado"));
            if (cita.getVeterinario() == null || !cita.getVeterinario().getId().equals(vet.getId())) {
                throw new ForbiddenException("No tienes acceso a esta cita");
            }
        }
    }

    /**
     * Valida que exista cupo segun el horario de atencion configurado por la empresa.
     * Si la empresa aun no configuro ningun horario, no se aplica restriccion (compatibilidad).
     */
    private void validarCupoDisponible(Empresa empresa, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        List<HorarioAtencion> horariosEmpresa = horarioAtencionRepository.findByEmpresaIdOrderByDiaSemana(empresa.getId());
        if (horariosEmpresa.isEmpty()) {
            return;
        }

        DayOfWeek diaSemana = fecha.getDayOfWeek();
        HorarioAtencion horario = horariosEmpresa.stream()
                .filter(h -> h.getDiaSemana() == diaSemana && Boolean.TRUE.equals(h.getActivo()))
                .findFirst()
                .orElse(null);

        if (horario == null) {
            throw new BusinessException("La empresa no atiende ese día.");
        }
        if (horaInicio.isBefore(horario.getHoraInicio()) || horaFin.isAfter(horario.getHoraFin())) {
            throw new BusinessException("El horario seleccionado está fuera del horario de atención.");
        }

        long ocupadas = citaRepository.findByEmpresaIdAndFechaProgramada(empresa.getId(), fecha).stream()
                .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA && c.getEstado() != AppointmentStatus.RECHAZADA)
                .filter(c -> c.getHoraInicio().isBefore(horaFin) && c.getHoraFin().isAfter(horaInicio))
                .count();

        if (ocupadas >= horario.getCapacidad()) {
            throw new BusinessException("Ya no hay cupo disponible en ese horario. Seleccione otro horario.");
        }
    }

    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(Long empresaId, Long servicioId, LocalDate fecha) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        DayOfWeek diaSemana = fecha.getDayOfWeek();
        HorarioAtencion horario = horarioAtencionRepository.findByEmpresaIdAndDiaSemana(empresaId, diaSemana)
                .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                .orElse(null);

        if (horario == null) {
            return List.of();
        }

        int duracion = servicio.getDuracionMinutos();
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = horario.getHoraInicio();
        while (!cursor.plusMinutes(duracion).isAfter(horario.getHoraFin())) {
            slots.add(cursor);
            cursor = cursor.plusMinutes(duracion);
        }

        if (slots.isEmpty()) {
            return List.of();
        }

        List<Cita> citasDelDia = citaRepository.findByEmpresaIdAndFechaProgramada(empresaId, fecha).stream()
                .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA && c.getEstado() != AppointmentStatus.RECHAZADA)
                .toList();

        List<LocalTime> disponibles = new ArrayList<>();
        for (LocalTime slotInicio : slots) {
            LocalTime slotFin = slotInicio.plusMinutes(duracion);
            long ocupadas = citasDelDia.stream()
                    .filter(c -> c.getHoraInicio().isBefore(slotFin) && c.getHoraFin().isAfter(slotInicio))
                    .count();
            if (ocupadas < horario.getCapacidad()) {
                disponibles.add(slotInicio);
            }
        }
        return disponibles;
    }
}
