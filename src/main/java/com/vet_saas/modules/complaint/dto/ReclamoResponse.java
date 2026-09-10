package com.vet_saas.modules.complaint.dto;

import com.vet_saas.modules.complaint.model.Reclamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclamoResponse {
    private Long id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String direccion;
    private String departamento;
    private String provincia;
    private String distrito;
    private String correo;
    private String telefono;
    private Boolean esMenor;
    private String numeroOrden;
    private BigDecimal montoReclamado;
    private String nombreProducto;
    private String tipoReclamo;
    private String resumen;
    private String archivoAdjuntoUrl;
    private String pdfReclamoUrl;
    private LocalDateTime fechaRegistro;

    public static ReclamoResponse fromEntity(Reclamo reclamo) {
        if (reclamo == null) return null;
        return ReclamoResponse.builder()
                .id(reclamo.getId())
                .tipoDocumento(reclamo.getTipoDocumento())
                .numeroDocumento(reclamo.getNumeroDocumento())
                .primerNombre(reclamo.getPrimerNombre())
                .segundoNombre(reclamo.getSegundoNombre())
                .primerApellido(reclamo.getPrimerApellido())
                .segundoApellido(reclamo.getSegundoApellido())
                .direccion(reclamo.getDireccion())
                .departamento(reclamo.getDepartamento())
                .provincia(reclamo.getProvincia())
                .distrito(reclamo.getDistrito())
                .correo(reclamo.getCorreo())
                .telefono(reclamo.getTelefono())
                .esMenor(reclamo.getEsMenor())
                .numeroOrden(reclamo.getNumeroOrden())
                .montoReclamado(reclamo.getMontoReclamado())
                .nombreProducto(reclamo.getNombreProducto())
                .tipoReclamo(reclamo.getTipoReclamo())
                .resumen(reclamo.getResumen())
                .archivoAdjuntoUrl(reclamo.getArchivoAdjuntoUrl())
                .pdfReclamoUrl(reclamo.getPdfReclamoUrl())
                .fechaRegistro(reclamo.getFechaRegistro())
                .build();
    }
}
