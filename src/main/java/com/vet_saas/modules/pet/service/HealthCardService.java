package com.vet_saas.modules.pet.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.medical_record.model.HistoriaClinica;
import com.vet_saas.modules.medical_record.repository.HistoriaClinicaRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import net.sf.jasperreports.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCardService {

    private final MascotaRepository mascotaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;

    private static final String JASPER_JRXML_PATH = "reports/health_card.jrxml";

    public byte[] generateHealthCard(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                        "Mascota", "id", mascotaId));

        try (InputStream is = new ClassPathResource(JASPER_JRXML_PATH).getInputStream()) {
            JasperReport jasperReport = JasperCompileManager.compileReport(is);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("nombreMascota", mascota.getNombre());
            parameters.put("especie", mascota.getEspecie());
            parameters.put("raza", mascota.getRaza() != null ? mascota.getRaza() : "No especificada");
            parameters.put("sexo", mascota.getSexo() != null ? mascota.getSexo().name() : "No especificado");
            parameters.put("peso", mascota.getPesoKg() != null ? mascota.getPesoKg().toString() + " kg" : "No registrado");
            parameters.put("fechaNacimiento", mascota.getFechaNacimiento() != null ?
                    mascota.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "No especificada");
            parameters.put("esterilizado", Boolean.TRUE.equals(mascota.getEsterilizado()) ? "Sí" : "No");
            parameters.put("observaciones", mascota.getObservacionesMedicas() != null ? mascota.getObservacionesMedicas() : "Ninguna");
            parameters.put("fechaEmision", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            List<HistoriaClinica> historiales = historiaClinicaRepository
                    .findByMascotaIdOrderByFechaRegistroDesc(mascotaId);

            StringBuilder historialText = new StringBuilder();
            int count = 0;
            for (HistoriaClinica h : historiales) {
                if (count >= 5) break;
                historialText.append(String.format("Fecha: %s | Diagnóstico: %s | Tratamiento: %s\n",
                        h.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        h.getDiagnostico() != null ? h.getDiagnostico() : "N/A",
                        h.getTratamiento() != null ? h.getTratamiento() : "N/A"));
                count++;
            }
            parameters.put("historialClinico", historialText.toString());

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            log.error("Error generando carnet de salud para mascota {}: {}", mascotaId, e.getMessage());
            throw new BusinessException("No se pudo generar el carnet de salud: " + e.getMessage());
        }
    }
}
