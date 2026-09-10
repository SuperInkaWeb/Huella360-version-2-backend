package com.vet_saas.modules.leads.dto;

import com.vet_saas.modules.leads.model.Lead;
import com.vet_saas.modules.leads.model.LeadEstado;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LeadResponse {
    private Long id;
    private Long empresaId;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;
    private String servicioSolicitado;
    private String mensaje;
    private LeadEstado estado;
    private LocalDateTime createdAt;

    public static LeadResponse fromEntity(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .empresaId(lead.getEmpresa().getId())
                .clienteNombre(lead.getClienteNombre())
                .clienteEmail(lead.getClienteEmail())
                .clienteTelefono(lead.getClienteTelefono())
                .servicioSolicitado(lead.getServicioSolicitado())
                .mensaje(lead.getMensaje())
                .estado(lead.getEstado())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}
