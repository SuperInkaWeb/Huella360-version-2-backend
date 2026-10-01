package com.vet_saas.modules.appointment.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.modules.appointment.dto.CitaRequest;
import com.vet_saas.modules.appointment.dto.CitaResponse;
import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.appointment.model.HorarioAtencion;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.appointment.repository.HorarioAtencionRepository;
import com.vet_saas.modules.catalog.model.Servicio;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V2: reserva con veterinario independiente (el cliente propone fecha/hora, el veterinario confirma
 * o rechaza) sin cambiar la reserva por horario de atencion de las empresas.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceReservaTest {

    @Mock private CitaRepository citaRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HorarioAtencionRepository horarioAtencionRepository;

    @InjectMocks private CitaService citaService;

    private static final LocalDate FECHA = LocalDate.now().plusDays(3);

    private Usuario cliente;
    private Veterinario vet;
    private Servicio servicioVet;
    private Empresa empresa;
    private Servicio servicioEmpresa;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().id(10L).correo("cliente@test.com").rol(Role.CLIENTE).build();
        vet = Veterinario.builder().id(2L).nombres("Carlos").apellidos("Rivas").build();
        servicioVet = Servicio.builder().id(20L).veterinario(vet).nombre("Consulta a domicilio")
                .precio(BigDecimal.TEN).duracionMinutos(45).activo(true).visible(true).build();
        empresa = Empresa.builder().id(3L).build();
        servicioEmpresa = Servicio.builder().id(30L).empresa(empresa).nombre("Baño")
                .precio(BigDecimal.TEN).duracionMinutos(30).activo(true).visible(true).build();
        lenient().when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
    }

    private CitaRequest request(Long servicioId, Long empresaId, LocalTime hora) {
        return CitaRequest.builder().servicioId(servicioId).empresaId(empresaId)
                .fechaProgramada(FECHA).horaInicio(hora).notasCliente("Mi perro cojea").build();
    }

    @Test
    void vetIndependiente_clientePropone_fechaYHora_seCreaSolicitadaAsignadaAlVetYSinEmpresa() {
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicioVet));

        CitaResponse resp = citaService.crearCita(cliente, request(20L, null, LocalTime.of(16, 0)));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(captor.capture());
        Cita cita = captor.getValue();
        assertNull(cita.getEmpresa());
        assertSame(vet, cita.getVeterinario());
        assertEquals(AppointmentStatus.SOLICITADA, cita.getEstado());
        assertEquals(LocalTime.of(16, 45), cita.getHoraFin());
        assertEquals("Carlos Rivas", resp.getVeterinarioNombre());
        // Sin horario fijo: nunca se consulta el horario de atencion ni una empresa
        verifyNoInteractions(horarioAtencionRepository, empresaRepository);
    }

    @Test
    void vetIndependiente_ignoraElEmpresaIdQueMandeElCliente_elServicioDefineAlVeterinario() {
        // El frontend anterior mandaba el id del veterinario como empresaId: no debe caer en otra empresa
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicioVet));

        citaService.crearCita(cliente, request(20L, 2L, LocalTime.of(10, 0)));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(captor.capture());
        assertNull(captor.getValue().getEmpresa());
        verifyNoInteractions(empresaRepository);
    }

    @Test
    void vetIndependiente_rechazaSiYaHayUnaCitaConfirmadaEnEseHorario() {
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicioVet));
        when(citaRepository.existsOverlapEnEstados(eq(2L), eq(FECHA), eq(LocalTime.of(16, 0)), eq(LocalTime.of(16, 45)),
                eq(List.of(AppointmentStatus.CONFIRMADA)), eq(0L))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> citaService.crearCita(cliente, request(20L, null, LocalTime.of(16, 0))));
        assertTrue(ex.getMessage().contains("confirmada"));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void vetIndependiente_rechazaMascotaDeOtroUsuario() {
        Usuario otro = Usuario.builder().id(77L).build();
        when(mascotaRepository.findById(5L)).thenReturn(Optional.of(Mascota.builder().id(5L).usuario(otro).build()));
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicioVet));
        CitaRequest req = request(20L, null, LocalTime.of(9, 0));
        req.setMascotaId(5L);

        assertThrows(ForbiddenException.class, () -> citaService.crearCita(cliente, req));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void vetIndependiente_rechazaServicioInactivo() {
        servicioVet.setActivo(false);
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(servicioVet));

        assertThrows(BusinessException.class, () -> citaService.crearCita(cliente, request(20L, null, LocalTime.of(9, 0))));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void vetIndependiente_alConfirmar_rechazaSiSeCruzaConOtraConfirmada() {
        Cita propuesta = Cita.builder().id(50L).veterinario(vet).servicio(servicioVet).cliente(cliente)
                .fechaProgramada(FECHA).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(16, 45))
                .estado(AppointmentStatus.SOLICITADA).build();
        when(citaRepository.findById(50L)).thenReturn(Optional.of(propuesta));
        when(citaRepository.existsOverlapEnEstados(2L, FECHA, LocalTime.of(16, 0), LocalTime.of(16, 45),
                List.of(AppointmentStatus.CONFIRMADA), 50L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> citaService.actualizarEstado(50L, AppointmentStatus.CONFIRMADA, null));
        assertEquals(AppointmentStatus.SOLICITADA, propuesta.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void vetIndependiente_alConfirmar_sinCruce_quedaConfirmada_yRechazarNoValidaCruces() {
        Cita propuesta = Cita.builder().id(50L).veterinario(vet).servicio(servicioVet).cliente(cliente)
                .fechaProgramada(FECHA).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(16, 45))
                .estado(AppointmentStatus.SOLICITADA).build();
        when(citaRepository.findById(50L)).thenReturn(Optional.of(propuesta));

        assertEquals(AppointmentStatus.CONFIRMADA,
                citaService.actualizarEstado(50L, AppointmentStatus.CONFIRMADA, null).getEstado());

        clearInvocations(citaRepository);
        propuesta.setEstado(AppointmentStatus.SOLICITADA);
        assertEquals(AppointmentStatus.RECHAZADA,
                citaService.actualizarEstado(50L, AppointmentStatus.RECHAZADA, "No atiendo ese dia").getEstado());
        verify(citaRepository, never()).existsOverlapEnEstados(any(), any(), any(), any(), any(), any());
    }

    @Test
    void empresa_servicioDeEmpresa_sinEmpresaId_esRechazado() {
        when(servicioRepository.findById(30L)).thenReturn(Optional.of(servicioEmpresa));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> citaService.crearCita(cliente, request(30L, null, LocalTime.of(10, 0))));
        assertEquals("La empresa es requerida", ex.getMessage());
    }

    @Test
    void empresa_servicioDeEmpresa_sigueValidandoElHorarioDeAtencion() {
        when(servicioRepository.findById(30L)).thenReturn(Optional.of(servicioEmpresa));
        when(empresaRepository.findById(3L)).thenReturn(Optional.of(empresa));
        HorarioAtencion horario = HorarioAtencion.builder().empresa(empresa).diaSemana(FECHA.getDayOfWeek())
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(13, 0)).capacidad(1).activo(true).build();
        when(horarioAtencionRepository.findByEmpresaIdOrderByDiaSemana(3L)).thenReturn(List.of(horario));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> citaService.crearCita(cliente, request(30L, 3L, LocalTime.of(16, 0))));
        assertEquals("El horario seleccionado está fuera del horario de atención.", ex.getMessage());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void empresa_servicioDeEmpresa_dentroDelHorario_seCreaConEmpresa() {
        when(servicioRepository.findById(30L)).thenReturn(Optional.of(servicioEmpresa));
        when(empresaRepository.findById(3L)).thenReturn(Optional.of(empresa));
        HorarioAtencion horario = HorarioAtencion.builder().empresa(empresa).diaSemana(FECHA.getDayOfWeek())
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(13, 0)).capacidad(2).activo(true).build();
        when(horarioAtencionRepository.findByEmpresaIdOrderByDiaSemana(3L)).thenReturn(List.of(horario));
        when(citaRepository.findByEmpresaIdAndFechaProgramada(3L, FECHA)).thenReturn(List.of());

        citaService.crearCita(cliente, request(30L, 3L, LocalTime.of(10, 0)));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(captor.capture());
        assertSame(empresa, captor.getValue().getEmpresa());
        assertNull(captor.getValue().getVeterinario());
    }

    @Test
    void empresa_confirmarCitaDeEmpresa_noAplicaLaValidacionDelVeterinarioIndependiente() {
        Cita citaEmpresa = Cita.builder().id(60L).empresa(empresa).veterinario(vet).servicio(servicioEmpresa)
                .cliente(cliente).fechaProgramada(FECHA).horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(10, 30))
                .estado(AppointmentStatus.SOLICITADA).build();
        when(citaRepository.findById(60L)).thenReturn(Optional.of(citaEmpresa));

        assertEquals(AppointmentStatus.CONFIRMADA,
                citaService.actualizarEstado(60L, AppointmentStatus.CONFIRMADA, null).getEstado());
        verify(citaRepository, never()).existsOverlapEnEstados(any(), any(), any(), any(), any(), any());
    }
}
