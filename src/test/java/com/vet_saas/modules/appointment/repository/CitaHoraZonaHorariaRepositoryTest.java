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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regresion: con la JVM en America/Lima (VetSaasApplication) y hibernate.jdbc.time_zone=UTC,
 * las columnas TIME de citas se guardaban y consultaban desplazadas 5 horas (16:00 -> 21:00).
 */
class CitaHoraZonaHorariaRepositoryTest extends AbstractIntegrationTest {

    @Autowired private CitaRepository citaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final LocalDate FECHA = LocalDate.now().plusDays(5);

    private TimeZone zonaOriginal;
    private Usuario cliente;
    private Usuario usuarioVet;
    private Veterinario vet;
    private Servicio servicio;
    private Cita cita;

    @BeforeEach
    void setUp() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));

        cliente = usuarioRepository.save(Usuario.builder().correo("cliente-tz@test.com").password("x")
                .rol(Role.CLIENTE).estado(true).emailVerificado(true).build());
        usuarioVet = usuarioRepository.save(Usuario.builder().correo("vet-tz@test.com").password("x")
                .rol(Role.VETERINARIO).estado(true).emailVerificado(true).build());
        vet = veterinarioRepository.save(Veterinario.builder().usuario(usuarioVet).nombres("Ana").apellidos("Torres").build());
        servicio = servicioRepository.save(Servicio.builder().veterinario(vet).nombre("Consulta general")
                .precio(BigDecimal.TEN).duracionMinutos(45).build());
    }

    @AfterEach
    void limpiarDatos() {
        if (cita != null) citaRepository.delete(cita);
        servicioRepository.delete(servicio);
        veterinarioRepository.delete(vet);
        usuarioRepository.delete(usuarioVet);
        usuarioRepository.delete(cliente);
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    void horaDeLaCita_seGuardaYConsultaSinDesplazamientoDeZona() {
        cita = citaRepository.saveAndFlush(Cita.builder().cliente(cliente).servicio(servicio)
                .veterinario(vet).fechaProgramada(FECHA).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(16, 45))
                .estado(AppointmentStatus.CONFIRMADA).build());

        // lo que queda en la columna TIME
        String enBase = jdbcTemplate.queryForObject("select hora_inicio::text from citas where id_cita = ?", String.class, cita.getId());
        assertEquals("16:00:00", enBase);

        // lectura por JPA
        assertEquals(LocalTime.of(16, 0), citaRepository.findById(cita.getId()).orElseThrow().getHoraInicio());

        // parametros LocalTime en consultas JPQL
        List<AppointmentStatus> confirmadas = List.of(AppointmentStatus.CONFIRMADA);
        assertTrue(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(16, 30), LocalTime.of(17, 15), confirmadas, 0L));
        assertFalse(citaRepository.existsOverlapEnEstados(vet.getId(), FECHA, LocalTime.of(21, 0), LocalTime.of(21, 45), confirmadas, 0L));
    }
}
