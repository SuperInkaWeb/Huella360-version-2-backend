package com.vet_saas.modules.geo.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.geo.dto.LugarCercanoResponse;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CercaniaServiceTest {

    // Punto de búsqueda: Plaza de Armas de Piura.
    private static final double LAT = -5.1945;
    private static final double LNG = -80.6328;

    @Mock private EmpresaRepository empresaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;

    private CercaniaService cercaniaService;

    @BeforeEach
    void setUp() {
        cercaniaService = new CercaniaService(empresaRepository, veterinarioRepository);
    }

    private Empresa empresa(long id, String nombre, String tipoServicio, double lat, double lng) {
        return Empresa.builder().id(id).nombreComercial(nombre).tipoServicio(tipoServicio)
                .direccion("Av. Grau " + id).ciudad("Piura").logoUrl("logo" + id)
                .ubicacionLat(BigDecimal.valueOf(lat)).ubicacionLng(BigDecimal.valueOf(lng)).build();
    }

    private Veterinario veterinario(long id, double lat, double lng) {
        return Veterinario.builder().id(id).nombres("Ana").apellidos("Ruiz").especialidad("Felinos")
                .direccion("Calle Lima " + id).ciudad("Piura").fotoPerfilUrl("foto" + id)
                .ubicacionLat(BigDecimal.valueOf(lat)).ubicacionLng(BigDecimal.valueOf(lng)).build();
    }

    private void empresasEnRecuadro(Empresa... empresas) {
        when(empresaRepository.buscarEnRecuadro(eq(VerificationStatus.VERIFICADO), any(), any(), any(), any()))
                .thenReturn(List.of(empresas));
    }

    private void veterinariosEnRecuadro(Veterinario... veterinarios) {
        when(veterinarioRepository.buscarEnRecuadro(eq(VerificationStatus.VERIFICADO), any(), any(), any(), any()))
                .thenReturn(List.of(veterinarios));
    }

    @Test
    void buscar_mezclaEmpresasYVeterinariosOrdenadosPorDistancia() {
        // ~2,2 km, ~0,6 km y ~1,1 km al norte del punto.
        empresasEnRecuadro(
                empresa(1, "Clínica Lejana", "VETERINARIA", LAT + 0.02, LNG),
                empresa(2, "Pet Shop Cerca", "PETSHOP", LAT + 0.005, LNG));
        veterinariosEnRecuadro(veterinario(7, LAT + 0.01, LNG));

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, null, null, 20);

        assertEquals(List.of("Pet Shop Cerca", "Ana Ruiz", "Clínica Lejana"),
                res.stream().map(LugarCercanoResponse::nombre).toList());
        LugarCercanoResponse vet = res.get(1);
        assertEquals(CercaniaService.TIPO_VETERINARIO, vet.tipo());
        assertEquals(7L, vet.id());
        assertEquals("Felinos", vet.categoria());
        assertEquals("foto7", vet.imagenUrl());
        assertEquals(CercaniaService.TIPO_EMPRESA, res.get(0).tipo());
        assertEquals("PETSHOP", res.get(0).categoria());
        assertEquals("logo2", res.get(0).imagenUrl());
        assertEquals(0.56, res.get(0).distanciaKm(), 0.01);
        assertEquals(1.11, vet.distanciaKm(), 0.01);
    }

    @Test
    void buscar_descartaLasEsquinasDelRecuadroFueraDelRadio() {
        // En diagonal: dentro del recuadro de 5 km pero a ~6,2 km del punto.
        empresasEnRecuadro(
                empresa(1, "En la esquina", "VETERINARIA", LAT + 0.04, LNG + 0.04),
                empresa(2, "Dentro", "VETERINARIA", LAT + 0.01, LNG));
        veterinariosEnRecuadro();

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, null, null, 20);

        assertEquals(List.of("Dentro"), res.stream().map(LugarCercanoResponse::nombre).toList());
    }

    @Test
    void buscar_consultaUnRecuadroDelTamanoDelRadio() {
        empresasEnRecuadro();
        veterinariosEnRecuadro();

        cercaniaService.buscar(LAT, LNG, 10, null, null, 20);

        ArgumentCaptor<BigDecimal> latMin = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> latMax = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> lngMin = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> lngMax = ArgumentCaptor.forClass(BigDecimal.class);
        verify(empresaRepository).buscarEnRecuadro(eq(VerificationStatus.VERIFICADO),
                latMin.capture(), latMax.capture(), lngMin.capture(), lngMax.capture());
        // 10 km son ~0,09 grados de latitud; cerca del ecuador, casi lo mismo en longitud.
        assertEquals(LAT - 0.0898, latMin.getValue().doubleValue(), 0.001);
        assertEquals(LAT + 0.0898, latMax.getValue().doubleValue(), 0.001);
        assertEquals(LNG - 0.0902, lngMin.getValue().doubleValue(), 0.001);
        assertEquals(LNG + 0.0902, lngMax.getValue().doubleValue(), 0.001);
    }

    @Test
    void buscar_respetaElLimite() {
        empresasEnRecuadro(
                empresa(1, "A", "VETERINARIA", LAT + 0.001, LNG),
                empresa(2, "B", "VETERINARIA", LAT + 0.002, LNG),
                empresa(3, "C", "VETERINARIA", LAT + 0.003, LNG));
        veterinariosEnRecuadro();

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, null, null, 2);

        assertEquals(List.of("A", "B"), res.stream().map(LugarCercanoResponse::nombre).toList());
    }

    @Test
    void buscar_tipoEmpresa_noConsultaVeterinarios() {
        empresasEnRecuadro(empresa(1, "A", "VETERINARIA", LAT, LNG));

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, "empresa", null, 20);

        assertEquals(1, res.size());
        verifyNoInteractions(veterinarioRepository);
    }

    @Test
    void buscar_tipoVeterinario_noConsultaEmpresas() {
        veterinariosEnRecuadro(veterinario(7, LAT, LNG));

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, "VETERINARIO", null, 20);

        assertEquals(1, res.size());
        assertEquals(0.0, res.get(0).distanciaKm());
        verifyNoInteractions(empresaRepository);
    }

    @Test
    void buscar_tipoServicio_filtraEmpresasYExcluyeVeterinarios() {
        empresasEnRecuadro(
                empresa(1, "Clínica", "VETERINARIA", LAT, LNG),
                empresa(2, "Pet Shop", "PETSHOP", LAT, LNG));

        List<LugarCercanoResponse> res = cercaniaService.buscar(LAT, LNG, 5, null, "veterinaria", 20);

        assertEquals(List.of("Clínica"), res.stream().map(LugarCercanoResponse::nombre).toList());
        verifyNoInteractions(veterinarioRepository);
    }

    @Test
    void buscar_sinResultados_devuelveListaVacia() {
        empresasEnRecuadro();
        veterinariosEnRecuadro();

        assertTrue(cercaniaService.buscar(LAT, LNG, 5, null, null, 20).isEmpty());
    }

    @Test
    void buscar_rechazaParametrosInvalidos() {
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(91, LNG, 5, null, null, 20));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, -181, 5, null, null, 20));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, LNG, 0, null, null, 20));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, LNG, 51, null, null, 20));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, LNG, 5, null, null, 0));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, LNG, 5, null, null, 101));
        assertThrows(BusinessException.class, () -> cercaniaService.buscar(LAT, LNG, 5, "CLIENTE", null, 20));
        verifyNoInteractions(empresaRepository, veterinarioRepository);
    }

    @Test
    void distanciaKm_piuraALima() {
        // Plaza de Armas de Piura a Plaza Mayor de Lima: ~860 km en línea recta.
        double km = CercaniaService.distanciaKm(LAT, LNG, BigDecimal.valueOf(-12.0464), BigDecimal.valueOf(-77.0300));
        assertEquals(860, km, 15);
    }
}
