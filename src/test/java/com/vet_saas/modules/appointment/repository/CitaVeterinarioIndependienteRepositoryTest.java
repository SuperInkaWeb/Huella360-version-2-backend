package com.vet_saas.modules.appointment.repository;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.catalog.model.Servicio;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2 (reserva con veterinario independiente) con BD real: la migracion V51 permite citas sin empresa
 * pero exige empresa o veterinario, y existsOverlapEnEstados funciona con el enum nativo appointment_status.
 */
class CitaVeterinarioIndependienteRepositoryTest extends AbstractIntegrationTest {

    @Autowired private CitaRepository citaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private static final LocalDate FECHA = LocalDate.now().plusDays(5);

    private final List<Cita> citas = new ArrayList<>();
    private Usuario cliente;
    private Usuario usuarioVet;
    private Veterinario vet;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        cliente = usuarioRepository.save(Usuario.builder().correo("cliente-v2@test.com").password("x")
                .rol(Role.CLIENTE).estado(true).emailVerificado(true).build());
        usuarioVet = usuarioRepository.save(Usuario.builder().correo("vet-v2@test.com").password("x")
                .rol(Role.VETERINARIO).estado(true).emailVerificado(true).build());
        vet = veterinarioRepository.save(Veterinario.builder().usuario(usuarioVet).nombres("Carlos").apellidos("Rivas").build());
        servicio = servicioRepository.save(Servicio.builder().veterinario(vet).nombre("Consulta a domicilio")
                .precio(BigDecimal.TEN).duracionMinutos(45).build());
    }

    @AfterEach
    void limpiarDatos() {
        citaRepository.deleteAll(citas);
        servicioRepository.delete(servicio);
        veterinarioRepository.delete(vet);
        usuarioRepository.delete(usuarioVet);
        usuarioRepository.delete(cliente);
    }

    private Cita guardar(Veterinario veterinario, LocalTime inicio, AppointmentStatus estado) {
        Cita cita = citaRepository.saveAndFlush(Cita.builder().cliente(cliente).servicio(servicio).empresa(null)
                .veterinario(veterinario).fechaProgramada(FECHA).horaInicio(inicio).horaFin(inicio.plusMinutes(45))
                .estado(estado).build());
        citas.add(cita);
        return cita;
    }

    @Test
    void citaSinEmpresa_conVeterinario_seGuarda_yElCruceSoloCuentaLosEstadosPedidos() {
        Cita confirmada = guardar(vet, LocalTime.of(16, 0), AppointmentStatus.CONFIRMADA);
        guardar(vet, LocalTime.of(18, 0), AppointmentStatus.SOLICITADA);

        assertNull(citaRepository.findById(confirmada.getId()).orElseThrow().getEmpresa());
        List<AppointmentStatus> soloConfirmadas = List.of(AppointmentStatus.CONFIRMADA);
        // 16:30-17:15 se cruza con la confirmada de 16:00-16:45
        assertTrue(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(16, 30), LocalTime.of(17, 15), soloConfirmadas, 0L));
        // excluyendo la propia cita (al confirmarla) no hay cruce
        assertFalse(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(16, 0), LocalTime.of(16, 45), soloConfirmadas, confirmada.getId()));
        // 18:00 solo tiene una propuesta SOLICITADA: no bloquea
        assertFalse(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(18, 0), LocalTime.of(18, 45), soloConfirmadas, 0L));
        // horario contiguo (16:45) no se cruza
        assertFalse(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(16, 45), LocalTime.of(17, 30), soloConfirmadas, 0L));
        // la agenda del veterinario la incluye
        assertEquals(2, citaRepository.findByVeterinarioId(vet.getId()).size());
    }

    @Test
    void citaSinEmpresaNiVeterinario_esRechazadaPorLaBase() {
        assertThrows(DataIntegrityViolationException.class, () -> guardar(null, LocalTime.of(10, 0), AppointmentStatus.SOLICITADA));
        citas.clear();
    }
}
