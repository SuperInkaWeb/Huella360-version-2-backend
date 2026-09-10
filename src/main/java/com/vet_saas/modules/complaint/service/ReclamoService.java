package com.vet_saas.modules.complaint.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.complaint.dto.ReclamoRequestDto;
import com.vet_saas.modules.complaint.model.EstadoReclamo;
import com.vet_saas.modules.complaint.model.Reclamo;
import com.vet_saas.modules.complaint.repository.ReclamoRepository;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReclamoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReclamoService.class);
    private final ReclamoRepository reclamoRepository;
    private final EmailService emailService;
    private final Cloudinary cloudinary;
    private final PdfService pdfService; // Inyectar el nuevo servicio de Jasper

    @Transactional
    public String registrarReclamo(Usuario usuario, ReclamoRequestDto dto, MultipartFile archivo) {
        String archivoUrl = null;

        // 1. Subir archivo de sustento del cliente (Opcional)
        if (archivo != null && !archivo.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(archivo.getBytes(), ObjectUtils.emptyMap());
                archivoUrl = uploadResult.get("url").toString();
            } catch (Exception e) {
                LOGGER.error("Error al subir archivo de sustento: {}", e.getMessage());
            }
        }

        // 2. Guardar entidad inicial para conseguir el ID autogenerado
        Reclamo reclamo = Reclamo.builder()
                .usuario(usuario)
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(dto.getNumeroDocumento())
                .primerNombre(dto.getPrimerNombre())
                .segundoNombre(dto.getSegundoNombre())
                .primerApellido(dto.getPrimerApellido())
                .segundoApellido(dto.getSegundoApellido())
                .direccion(dto.getDireccion())
                .departamento(dto.getDepartamento())
                .provincia(dto.getProvincia())
                .distrito(dto.getDistrito())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .esMenor(dto.getEsMenor())
                .apoderadoTipoDocumento(dto.getApoderadoTipoDocumento())
                .apoderadoNumeroDocumento(dto.getApoderadoNumeroDocumento())
                .apoderadoPrimerNombre(dto.getApoderadoPrimerNombre())
                .apoderadoPrimerApellido(dto.getApoderadoPrimerApellido())
                .apoderadoCorreo(dto.getApoderadoCorreo())
                .apoderadoTelefono(dto.getApoderadoTelefono())
                .numeroOrden(dto.getNumeroOrden())
                .montoReclamado(dto.getMontoReclamado() != null && !dto.getMontoReclamado().isEmpty() ? new BigDecimal(dto.getMontoReclamado()) : null)
                .nombreProducto(dto.getNombreProducto())
                .tipoReclamo(dto.getTipoReclamo())
                .resumen(dto.getResumen())
                .detallePedido(dto.getDetallePedido())
                .archivoAdjuntoUrl(archivoUrl)
                .build();

        reclamo = reclamoRepository.save(reclamo);

        // 3. Generar el PDF con JasperReports
        byte[] pdfBytes = pdfService.generateReclamoPdf(dto, reclamo.getId());

        // 4. Subir el PDF generado a Cloudinary
        String pdfUrl = subirPdfACloudinary(pdfBytes, reclamo.getId());
        LOGGER.info("PDF subido exitosamente a Cloudinary: {}", pdfUrl);

        // 5. Actualizar base de datos con la URL en formato TEXT
        reclamo.setPdfReclamoUrl(pdfUrl);
        reclamo = reclamoRepository.save(reclamo);

        // 6. Enviar Correo Electrónico pasándole la URL
        String nombreCliente = dto.getPrimerNombre() + " " + dto.getPrimerApellido();
        emailService.sendReclamoEmailConLink(dto.getCorreo(), nombreCliente, "Reclamo N° " + String.format("%06d", reclamo.getId()), pdfUrl);

        return reclamo.getPdfReclamoUrl();
    }

    @Transactional
    public Reclamo actualizarEstado(Long reclamoId, EstadoReclamo nuevoEstado, String notasInternas) {
        Reclamo reclamo = reclamoRepository.findById(reclamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamo", "id", reclamoId));

        reclamo.setEstado(nuevoEstado);
        if (notasInternas != null) {
            reclamo.setNotasInternas(notasInternas);
        }

        return reclamoRepository.save(reclamo);
    }

    private String subirPdfACloudinary(byte[] pdfBytes, Long reclamoId) {
        try {
            // Usamos "image" y format "pdf" para que Cloudinary permita visualizarlo en el navegador
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", "reclamos/reclamo_" + String.format("%06d", reclamoId),
                    "format", "pdf",
                    "flags", "attachment"
            );

            Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, options);
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            LOGGER.error("Error al subir el PDF de reclamo a Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException("No se pudo guardar el documento generado");
        }
    }
}