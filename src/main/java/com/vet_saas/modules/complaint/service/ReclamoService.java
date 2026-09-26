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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Resultado del registro: el numero siempre existe; la URL del PDF puede faltar si fallo su generacion. */
    public record ResultadoReclamo(Long id, String numero, String pdfUrl) {}

    public static String formatearNumero(Long id) {
        return String.format("%06d", id);
    }

    /**
     * Registra un reclamo del Libro de Reclamaciones. Puede llamarlo un usuario anonimo (usuario == null).
     *
     * El reclamo se guarda PRIMERO y ese guardado no depende de nada externo. Antes todo el metodo era
     * una sola transaccion: si fallaba la subida del PDF a Cloudinary, se deshacia tambien el reclamo y el
     * consumidor lo perdia. Ahora el PDF, su subida y los correos son pasos posteriores "best effort":
     * si fallan se registra el error y el reclamo queda igual (visible para el admin en GET /reclamos).
     */
    public ResultadoReclamo registrarReclamo(Usuario usuario, ReclamoRequestDto dto, MultipartFile archivo) {
        String archivoUrl = null;

        // 1. Subir archivo de sustento del cliente (opcional). El SDK de Cloudinary acepta byte[], File o
        //    String, NO InputStream ("Unrecognized file parameter"): antes esta subida fallaba siempre.
        if (archivo != null && !archivo.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(archivo.getBytes(),
                        ObjectUtils.asMap("resource_type", "auto", "folder", "reclamos/adjuntos"));
                archivoUrl = String.valueOf(uploadResult.get("secure_url"));
            } catch (Exception e) {
                LOGGER.error("Error al subir archivo de sustento del reclamo: {}", e.getMessage(), e);
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
        String numero = formatearNumero(reclamo.getId());
        LOGGER.info("Reclamo N° {} registrado (usuario: {})", numero, usuario != null ? usuario.getId() : "anonimo");

        // 3-5. PDF (JasperReports) + subida a Cloudinary + URL en la BD. Best effort.
        String pdfUrl = null;
        try {
            byte[] pdfBytes = pdfService.generateReclamoPdf(dto, reclamo.getId());
            pdfUrl = subirPdfACloudinary(pdfBytes, reclamo.getId());
            reclamo.setPdfReclamoUrl(pdfUrl);
            reclamoRepository.save(reclamo);
            LOGGER.info("PDF del reclamo N° {} subido a Cloudinary: {}", numero, pdfUrl);
        } catch (Exception e) {
            LOGGER.error("Reclamo N° {} guardado, pero fallo la generacion/subida del PDF: {}", numero, e.getMessage(), e);
        }

        // 6. Correo al consumidor + copia administrativa (ADMIN_EMAIL). Asincrono y best effort.
        try {
            String nombreCliente = dto.getPrimerNombre() + " " + dto.getPrimerApellido();
            emailService.sendReclamoEmailConLink(dto.getCorreo(), nombreCliente, "Reclamo N° " + numero, pdfUrl);
        } catch (Exception e) {
            LOGGER.error("Reclamo N° {} guardado, pero no se pudo encolar el correo: {}", numero, e.getMessage(), e);
        }

        return new ResultadoReclamo(reclamo.getId(), numero, pdfUrl);
    }

    @Transactional(readOnly = true)
    public Page<Reclamo> listar(EstadoReclamo estado, Pageable pageable) {
        return estado == null
                ? reclamoRepository.findAllByOrderByFechaRegistroDesc(pageable)
                : reclamoRepository.findByEstadoOrderByFechaRegistroDesc(estado, pageable);
    }

    @Transactional(readOnly = true)
    public Reclamo obtener(Long id) {
        return reclamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamo", "id", id));
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
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", "reclamos/reclamo_" + String.format("%06d", reclamoId),
                    "format", "pdf",
                    "flags", "attachment"
            );

            // byte[]: el SDK no acepta InputStream (ver registrarReclamo)
            Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, options);
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            LOGGER.error("Error al subir el PDF de reclamo a Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException("No se pudo guardar el documento generado");
        }
    }
}