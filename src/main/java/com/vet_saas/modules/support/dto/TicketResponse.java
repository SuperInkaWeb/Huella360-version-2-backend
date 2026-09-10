package com.vet_saas.modules.support.dto;

import com.vet_saas.modules.support.model.TicketPriority;
import com.vet_saas.modules.support.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private UUID publicId;
    private String asunto;
    private String descripcion;
    private TicketStatus estado;
    private TicketPriority prioridad;
    private String categoria;
    private LocalDateTime createdAt;
}
