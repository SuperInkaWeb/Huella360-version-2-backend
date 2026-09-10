package com.vet_saas.modules.teleconsulta.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.teleconsulta.dto.ChatMensajeResponse;
import com.vet_saas.modules.teleconsulta.dto.ConsultaResponse;
import com.vet_saas.modules.teleconsulta.model.ChatMensaje;
import com.vet_saas.modules.teleconsulta.model.Consulta;
import com.vet_saas.modules.teleconsulta.model.ConsultaEstado;
import com.vet_saas.modules.teleconsulta.model.MensajeTipo;
import com.vet_saas.modules.teleconsulta.repository.ChatMensajeRepository;
import com.vet_saas.modules.teleconsulta.repository.ConsultaRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ChatMensajeRepository chatMensajeRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final MascotaRepository mascotaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ConsultaResponse createConsulta(Usuario cliente, Long veterinarioId, Long mascotaId, String mensajeInicial) {
        Veterinario veterinario = veterinarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", "id", veterinarioId));

        Mascota mascota = null;
        if (mascotaId != null) {
            mascota = mascotaRepository.findById(mascotaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mascota", "id", mascotaId));
        }

        String jitsiRoomId = "huella360-" + UUID.randomUUID().toString().substring(0, 8);

        Consulta consulta = Consulta.builder()
                .cliente(cliente)
                .veterinario(veterinario)
                .mascota(mascota)
                .estado(ConsultaEstado.PENDIENTE)
                .jitsiRoomId(jitsiRoomId)
                .build();

        Consulta saved = consultaRepository.save(consulta);

        if (mensajeInicial != null && !mensajeInicial.isBlank()) {
            ChatMensaje mensaje = ChatMensaje.builder()
                    .consulta(saved)
                    .remitente(cliente)
                    .contenido(mensajeInicial)
                    .tipo(MensajeTipo.TEXTO)
                    .build();
            chatMensajeRepository.save(mensaje);
        }

        log.info("Consulta {} creada por cliente {} para veterinario {}", saved.getId(), cliente.getId(), veterinarioId);
        return ConsultaResponse.fromEntity(saved);
    }

    @Transactional
    public ConsultaResponse acceptConsulta(Usuario veterinario, Long consultaId) {
        Consulta consulta = getConsultaForVeterinario(veterinario, consultaId);

        if (consulta.getEstado() != ConsultaEstado.PENDIENTE) {
            throw new BusinessException("Solo se pueden aceptar consultas en estado PENDIENTE");
        }

        consulta.setEstado(ConsultaEstado.ACEPTADA);
        Consulta saved = consultaRepository.save(consulta);

        sendSystemMessage(saved, "Consulta aceptada. Puede iniciar la videollamada.");

        return ConsultaResponse.fromEntity(saved);
    }

    @Transactional
    public ConsultaResponse startConsulta(Usuario veterinario, Long consultaId) {
        Consulta consulta = getConsultaForVeterinario(veterinario, consultaId);

        if (consulta.getEstado() != ConsultaEstado.ACEPTADA) {
            throw new BusinessException("Solo se pueden iniciar consultas aceptadas");
        }

        consulta.setEstado(ConsultaEstado.EN_CURSO);
        Consulta saved = consultaRepository.save(consulta);

        sendSystemMessage(saved, "Consulta iniciada. Videollamada disponible.");

        return ConsultaResponse.fromEntity(saved);
    }

    @Transactional
    public ConsultaResponse finishConsulta(Usuario usuario, Long consultaId) {
        Consulta consulta = getConsultaForUser(usuario, consultaId);

        if (consulta.getEstado() != ConsultaEstado.EN_CURSO) {
            throw new BusinessException("Solo se pueden finalizar consultas en curso");
        }

        consulta.setEstado(ConsultaEstado.FINALIZADA);
        Consulta saved = consultaRepository.save(consulta);

        sendSystemMessage(saved, "Consulta finalizada.");

        return ConsultaResponse.fromEntity(saved);
    }

    @Transactional
    public ConsultaResponse cancelConsulta(Usuario usuario, Long consultaId) {
        Consulta consulta = getConsultaForUser(usuario, consultaId);

        if (consulta.getEstado() == ConsultaEstado.FINALIZADA || consulta.getEstado() == ConsultaEstado.CANCELADA) {
            throw new BusinessException("No se puede cancelar una consulta finalizada o cancelada");
        }

        consulta.setEstado(ConsultaEstado.CANCELADA);
        Consulta saved = consultaRepository.save(consulta);

        sendSystemMessage(saved, "Consulta cancelada.");

        return ConsultaResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> getMyConsultas(Usuario usuario) {
        List<Consulta> consultas;
        if ("CLIENTE".equals(usuario.getRol().name())) {
            consultas = consultaRepository.findByClienteIdOrderByCreatedAtDesc(usuario.getId());
        } else {
            var vet = veterinarioRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new BusinessException("No se encontró perfil de veterinario"));
            consultas = consultaRepository.findByVeterinarioIdOrderByCreatedAtDesc(vet.getId());
        }
        return consultas.stream()
                .map(ConsultaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMensajeResponse> getMensajes(Usuario usuario, Long consultaId) {
        Consulta consulta = getConsultaForUser(usuario, consultaId);
        return chatMensajeRepository.findByConsultaIdOrderByCreatedAtAsc(consulta.getId()).stream()
                .map(ChatMensajeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMensajeResponse sendMessage(Usuario usuario, Long consultaId, String contenido, String tipo) {
        Consulta consulta = getConsultaForUser(usuario, consultaId);

        if (consulta.getEstado() == ConsultaEstado.CANCELADA || consulta.getEstado() == ConsultaEstado.FINALIZADA) {
            throw new BusinessException("No se pueden enviar mensajes en una consulta finalizada o cancelada");
        }

        MensajeTipo mensajeTipo = MensajeTipo.TEXTO;
        if (tipo != null) {
            try {
                mensajeTipo = MensajeTipo.valueOf(tipo.toUpperCase());
            } catch (IllegalArgumentException e) {
                mensajeTipo = MensajeTipo.TEXTO;
            }
        }

        ChatMensaje mensaje = ChatMensaje.builder()
                .consulta(consulta)
                .remitente(usuario)
                .contenido(contenido)
                .tipo(mensajeTipo)
                .build();

        ChatMensaje saved = chatMensajeRepository.save(mensaje);

        ChatMensajeResponse response = ChatMensajeResponse.fromEntity(saved);
        messagingTemplate.convertAndSend("/topic/consultas/" + consultaId + "/mensajes", response);

        return response;
    }

    private void sendSystemMessage(Consulta consulta, String contenido) {
        ChatMensaje mensaje = ChatMensaje.builder()
                .consulta(consulta)
                .remitente(consulta.getCliente())
                .contenido(contenido)
                .tipo(MensajeTipo.SISTEMA)
                .build();
        chatMensajeRepository.save(mensaje);
    }

    private Consulta getConsultaForUser(Usuario usuario, Long consultaId) {
        if ("CLIENTE".equals(usuario.getRol().name())) {
            return consultaRepository.findByIdAndClienteId(consultaId, usuario.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta", "id", consultaId));
        } else {
            var vet = veterinarioRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new BusinessException("No se encontró perfil de veterinario"));
            return consultaRepository.findByIdAndVeterinarioId(consultaId, vet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta", "id", consultaId));
        }
    }

    private Consulta getConsultaForVeterinario(Usuario veterinario, Long consultaId) {
        var vet = veterinarioRepository.findByUsuarioId(veterinario.getId())
                .orElseThrow(() -> new BusinessException("No se encontró perfil de veterinario"));
        return consultaRepository.findByIdAndVeterinarioId(consultaId, vet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consulta", "id", consultaId));
    }
}
