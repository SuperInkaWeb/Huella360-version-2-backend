package com.vet_saas.modules.veterinarian.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.utils.CryptoUtil;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.dto.VeterinarioRequest;
import com.vet_saas.modules.veterinarian.dto.VeterinarioResponse;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H360-GEO-002: el veterinario independiente guarda su direccion y su punto en el mapa.
 */
@ExtendWith(MockitoExtension.class)
class VeterinarioServiceTest {

    private static final BigDecimal LAT = new BigDecimal("-5.19449000");
    private static final BigDecimal LNG = new BigDecimal("-80.63282000");

    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private CryptoUtil cryptoUtil;

    private VeterinarioService veterinarioService;

    private final Usuario usuario = Usuario.builder().id(20L).correo("vet@test.com").rol(Role.VETERINARIO).build();

    @BeforeEach
    void setUp() {
        veterinarioService = new VeterinarioService(veterinarioRepository, usuarioRepository, mascotaRepository,
                cryptoUtil);
    }

    private VeterinarioRequest request(String direccion, String ciudad, BigDecimal latitud, BigDecimal longitud) {
        return new VeterinarioRequest("Ana", "Rojas", "Medicina Interna", "12345", null, 5, null, null, null,
                direccion, ciudad, latitud, longitud);
    }

    private Veterinario vetGuardado() {
        return Veterinario.builder().id(5L).usuario(usuario).nombres("Ana").apellidos("Rojas")
                .numeroColegiatura("12345").direccion("Av. Grau 100").ciudad("Piura")
                .ubicacionLat(LAT).ubicacionLng(LNG).build();
    }

    @Test
    void crearPerfil_guardaLaDireccionYElPuntoDelMapa() {
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        VeterinarioResponse respuesta = veterinarioService.createProfile(20L,
                request("Av. Grau 100", "Piura", LAT, LNG));

        assertEquals("Av. Grau 100", respuesta.direccion());
        assertEquals("Piura", respuesta.ciudad());
        assertEquals(LAT, respuesta.latitud());
        assertEquals(LNG, respuesta.longitud());
    }

    @Test
    void crearPerfil_sinUbicacion_siguePermitido() {
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        VeterinarioResponse respuesta = veterinarioService.createProfile(20L, request(null, null, null, null));

        assertNull(respuesta.latitud());
        assertNull(respuesta.longitud());
    }

    @Test
    void crearPerfil_conSoloLatitud_seRechazaSinGuardar() {
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> veterinarioService.createProfile(20L, request(null, null, LAT, null)));

        verify(veterinarioRepository, never()).save(any());
    }

    @Test
    void actualizarPerfil_cambiaElPuntoDelMapa() {
        BigDecimal nuevaLat = new BigDecimal("-12.04637400");
        BigDecimal nuevaLng = new BigDecimal("-77.04279300");
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.of(vetGuardado()));
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        VeterinarioResponse respuesta = veterinarioService.updateProfile(20L,
                request("Jr. de la Unión 300", "Lima", nuevaLat, nuevaLng));

        assertEquals("Jr. de la Unión 300", respuesta.direccion());
        assertEquals("Lima", respuesta.ciudad());
        assertEquals(nuevaLat, respuesta.latitud());
        assertEquals(nuevaLng, respuesta.longitud());
    }

    @Test
    void actualizarPerfil_sinEnviarUbicacion_conservaLaGuardada() {
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.of(vetGuardado()));
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        VeterinarioResponse respuesta = veterinarioService.updateProfile(20L, request(null, null, null, null));

        assertEquals("Av. Grau 100", respuesta.direccion());
        assertEquals(LAT, respuesta.latitud());
        assertEquals(LNG, respuesta.longitud());
    }

    @Test
    void actualizarPerfil_conSoloLongitud_seRechazaSinGuardar() {
        when(veterinarioRepository.findByUsuarioId(20L)).thenReturn(Optional.of(vetGuardado()));

        assertThrows(BusinessException.class,
                () -> veterinarioService.updateProfile(20L, request(null, null, null, LNG)));

        verify(veterinarioRepository, never()).save(any());
    }
}
