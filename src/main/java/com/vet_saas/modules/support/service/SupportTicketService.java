package com.vet_saas.modules.support.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.support.dto.CreateTicketRequest;
import com.vet_saas.modules.support.dto.TicketResponse;
import com.vet_saas.modules.support.model.SupportTicket;
import com.vet_saas.modules.support.repository.SupportTicketRepository;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Canal de soporte del negocio: la clínica puede abrir tickets a la plataforma,
 * de forma separada al chat con sus clientes.
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;

    @Transactional
    public TicketResponse crearTicket(Usuario usuario, CreateTicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
                .usuario(usuario)
                .asunto(request.getAsunto())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .build();
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMisTickets(Usuario usuario) {
        return ticketRepository.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Usuario usuario, UUID publicId) {
        SupportTicket ticket = ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        // Seguridad: el ticket debe pertenecer al usuario autenticado.
        if (!ticket.getUsuario().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Ticket no encontrado");
        }
        return toResponse(ticket);
    }

    private TicketResponse toResponse(SupportTicket t) {
        return TicketResponse.builder()
                .publicId(t.getPublicId())
                .asunto(t.getAsunto())
                .descripcion(t.getDescripcion())
                .estado(t.getEstado())
                .prioridad(t.getPrioridad())
                .categoria(t.getCategoria())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
