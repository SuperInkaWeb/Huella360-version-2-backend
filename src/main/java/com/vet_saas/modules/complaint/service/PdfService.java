package com.vet_saas.modules.complaint.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.complaint.dto.ReclamoRequestDto;
import net.sf.jasperreports.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfService { // O JasperPdfService

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    // Apuntamos al archivo .jasper que ya está compilado
    private static final String JASPER_PATH = "reports/hoja_reclamo.jasper";

    public byte[] generateReclamoPdf(ReclamoRequestDto dto, Long reclamoId) {
        try (InputStream is = new ClassPathResource(JASPER_PATH).getInputStream()) {

            // 1. Cargar el reporte compilado
            JasperReport jasperReport = (JasperReport) net.sf.jasperreports.engine.util.JRLoader.loadObject(is);

            // 2. Preparar los parámetros
            Map<String, Object> parameters = new HashMap<>();

            // --- Cabecera ---
            parameters.put("fechaActual", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            parameters.put("numeroReclamo", String.format("%06d", reclamoId));

            // --- 1. Identificación del Consumidor ---
            String nombreCompleto = dto.getPrimerNombre() + " " +
                    (dto.getSegundoNombre() != null ? dto.getSegundoNombre() + " " : "") +
                    dto.getPrimerApellido() + " " +
                    (dto.getSegundoApellido() != null ? dto.getSegundoApellido() : "");
            parameters.put("nombreCompleto", nombreCompleto.toUpperCase());

            parameters.put("documento", dto.getTipoDocumento() + " - " + dto.getNumeroDocumento());

            String domicilio = dto.getDireccion() + ", " + dto.getDistrito() + " - " +
                    dto.getProvincia() + " - " + dto.getDepartamento();
            parameters.put("domicilio", domicilio.toUpperCase());

            parameters.put("telefono", dto.getTelefono());
            parameters.put("correo", dto.getCorreo());

            String apoderado = "";
            if (Boolean.TRUE.equals(dto.getEsMenor()) && dto.getApoderadoPrimerNombre() != null) {
                apoderado = dto.getApoderadoPrimerNombre() + " " + dto.getApoderadoPrimerApellido() +
                        " (" + dto.getApoderadoTipoDocumento() + ": " + dto.getApoderadoNumeroDocumento() + ")";
            }
            parameters.put("apoderado", apoderado.toUpperCase());

            // --- 2. Identificación del Bien Contratado ---
            boolean esQueja = dto.getTipoReclamo() != null && dto.getTipoReclamo().equalsIgnoreCase("QUEJA");
            parameters.put("esProducto", !esQueja ? "X" : "");
            parameters.put("esServicio", esQueja ? "X" : "");

            parameters.put("montoReclamado", "S/ " + (dto.getMontoReclamado() != null ? dto.getMontoReclamado() : "0.00"));

            String descripcionBien = (dto.getNombreProducto() != null ? dto.getNombreProducto() : "N/A") +
                    (dto.getNumeroOrden() != null ? " (Orden: " + dto.getNumeroOrden() + ")" : "");
            parameters.put("descripcionBien", descripcionBien.toUpperCase());

            // --- 3. Detalle de la Reclamación ---
            parameters.put("esReclamo", !esQueja ? "X" : "");
            parameters.put("esQueja", esQueja ? "X" : "");
            parameters.put("resumen", dto.getResumen());
            parameters.put("detalle", dto.getDetallePedido());

            // 3. Llenar el reporte (Usamos JREmptyDataSource porque no iteramos listas de BD)
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // 4. Exportar a Byte Array
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            LOGGER.error("Error al generar el PDF con JasperReports: {}", e.getMessage(), e);
            throw new BusinessException("No se pudo generar el PDF del reclamo");
        }
    }
}