package com.vet_saas.modules.company.dto;

import java.math.BigDecimal;

public record CompanyResponse(
                Long id,
                Long propietarioId,
                String nombreComercial,
                String razonSocial,
                String ruc,
                String descripcion,
                String tipoServicio,
                String telefono,
                String emailContacto,
                String direccion,
                String ciudad,
                String pais,
                BigDecimal latitud,
                BigDecimal longitud,
                String logoUrl,
                String bannerUrl,
                String mpPublicKey,
                String estadoValidacion) {
}