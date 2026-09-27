package com.vet_saas.modules.pet.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.medical_record.model.HistoriaClinica;
import com.vet_saas.modules.medical_record.repository.HistoriaClinicaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.model.Sexo;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Compila la plantilla real reports/health_card.jrxml con JasperReports 7:
 * si la plantilla vuelve a quedar en un formato que la librería no entiende, este test falla.
 */
@ExtendWith(MockitoExtension.class)
class HealthCardServiceTest {

    @Mock private MascotaRepository mascotaRepository;
    @Mock private HistoriaClinicaRepository historiaClinicaRepository;

    @InjectMocks private HealthCardService healthCardService;

    @Test
    void generateHealthCard_generaPdfConDatosDeLaMascotaYSuHistorial() throws Exception {
        Mascota mascota = Mascota.builder()
                .id(1L).nombre("Max").especie("Perro").raza("Labrador").sexo(Sexo.MACHO)
                .pesoKg(new BigDecimal("12.5")).fechaNacimiento(LocalDate.of(2022, 3, 15))
                .esterilizado(true).activo(true).build();
        HistoriaClinica historia = HistoriaClinica.builder()
                .fechaRegistro(LocalDateTime.of(2026, 9, 20, 10, 0))
                .diagnostico("Otitis").tratamiento("Gotas óticas").build();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(historiaClinicaRepository.findByMascotaIdOrderByFechaRegistroDesc(1L)).thenReturn(List.of(historia));

        byte[] pdf = healthCardService.generateHealthCard(1L);

        String texto;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            texto = new PDFTextStripper().getText(doc);
        }
        assertTrue(texto.contains("Carnet de Salud"));
        assertTrue(texto.contains("Max"));
        assertTrue(texto.contains("Labrador"));
        assertTrue(texto.contains("12.5 kg"));
        assertTrue(texto.contains("15/03/2022"));
        assertTrue(texto.contains("Otitis"));
        assertTrue(texto.contains("Fecha de emisión:"));
    }

    @Test
    void generateHealthCard_mascotaSinDatosOpcionalesNiHistorial_generaPdf() throws Exception {
        Mascota mascota = Mascota.builder().id(2L).nombre("Luna").especie("Gato").activo(true).build();
        when(mascotaRepository.findById(2L)).thenReturn(Optional.of(mascota));
        when(historiaClinicaRepository.findByMascotaIdOrderByFechaRegistroDesc(2L)).thenReturn(List.of());

        byte[] pdf = healthCardService.generateHealthCard(2L);

        String texto;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            texto = new PDFTextStripper().getText(doc);
        }
        assertTrue(texto.contains("Luna"));
        assertTrue(texto.contains("No especificada"));
    }

    @Test
    void generateHealthCard_mascotaInexistente_lanzaNotFound() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> healthCardService.generateHealthCard(99L));
    }
}
