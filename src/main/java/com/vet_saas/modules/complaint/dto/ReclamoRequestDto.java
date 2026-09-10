package com.vet_saas.modules.complaint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReclamoRequestDto {
    @NotBlank(message = "El tipo de documento es requerido")
    private String tipoDocumento;
    
    @NotBlank(message = "El número de documento es requerido")
    private String numeroDocumento;
    
    @NotBlank(message = "El primer nombre es requerido")
    private String primerNombre;
    
    private String segundoNombre;
    
    @NotBlank(message = "El primer apellido es requerido")
    private String primerApellido;
    
    private String segundoApellido;
    
    @NotBlank(message = "La dirección es requerida")
    private String direccion;
    
    @NotBlank(message = "El departamento es requerido")
    private String departamento;
    
    @NotBlank(message = "La provincia es requerida")
    private String provincia;
    
    @NotBlank(message = "El distrito es requerido")
    private String distrito;
    
    @NotBlank(message = "El correo es requerido")
    @Email(message = "El correo debe ser válido")
    private String correo;
    
    private String confirmacionCorreo;
    
    @NotBlank(message = "El teléfono es requerido")
    private String telefono;
    
    @NotNull(message = "Indique si es menor de edad")
    private Boolean esMenor;

    // Apoderado (Condicional en validación manual o en front)
    private String apoderadoTipoDocumento;
    private String apoderadoNumeroDocumento;
    private String apoderadoPrimerNombre;
    private String apoderadoSegundoNombre;
    private String apoderadoPrimerApellido;
    private String apoderadoSegundoApellido;
    private String apoderadoCorreo;
    private String apoderadoConfirmacionCorreo;
    private String apoderadoTelefono;

    // Info General
    private String numeroOrden;
    private String montoReclamado;
    private String nombreProducto;

    // Detalle
    @NotBlank(message = "El tipo de reclamo es requerido")
    private String tipoReclamo;
    
    @NotBlank(message = "El resumen es requerido")
    private String resumen;
    
    @NotBlank(message = "El detalle del pedido es requerido")
    private String detallePedido;
}
