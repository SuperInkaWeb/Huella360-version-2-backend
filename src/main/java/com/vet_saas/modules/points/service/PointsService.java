package com.vet_saas.modules.points.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.points.dto.ClientPointsDashboardDto;
import com.vet_saas.modules.points.dto.PointHistoryDto;
import com.vet_saas.modules.points.model.HistorialPuntos;
import com.vet_saas.modules.points.model.PuntosCliente;
import com.vet_saas.modules.points.repository.HistorialPuntosRepository;
import com.vet_saas.modules.points.repository.PuntosClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final PuntosClienteRepository puntosClienteRepository;
    private final HistorialPuntosRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final PointsConfigService configService;

    @Transactional
    public void addPoints(Long idPerfil, String accion, Long referenciaId, String descripcionPersonalizada) {
        int amount = configService.getPointsForAction(accion);
        
        if (amount <= 0) {
            return; // Action is disabled or grants 0 points
        }

        // Check for duplicates on one-time actions
        if (isOneTimeAction(accion) && historialRepository.existsByPuntosCliente_IdAndTipoAccion(idPerfil, accion)) {
            return;
        }

        // Check for duplicate rewards on the same reference entity (e.g. same order)
        if (referenciaId != null && historialRepository.existsByPuntosCliente_IdAndTipoAccionAndReferenciaId(idPerfil, accion, referenciaId)) {
            return;
        }

        PuntosCliente balance = getOrCreateBalance(idPerfil);
        
        balance.setPuntosTotales(balance.getPuntosTotales() + amount);
        balance.setUpdatedAt(LocalDateTime.now());
        
        // Final save for both new and existing balances
        balance = puntosClienteRepository.save(balance);

        HistorialPuntos historial = HistorialPuntos.builder()
                .puntosCliente(balance)
                .puntos(amount)
                .tipoAccion(accion)
                .referenciaId(referenciaId)
                .descripcion(descripcionPersonalizada != null ? descripcionPersonalizada : "Puntos por " + accion)
                .fecha(LocalDateTime.now())
                .build();
                
        historialRepository.save(historial);
    }

    @Transactional
    public void deductPoints(Long idPerfil, int amount, String accion, Long referenciaId, String descripcion) {
        if (amount <= 0) return;

        PuntosCliente balance = getOrCreateBalance(idPerfil);

        if (balance.getPuntosTotales() < amount) {
            throw new BusinessException("Puntos insuficientes para realizar esta acción");
        }

        balance.setPuntosTotales(balance.getPuntosTotales() - amount);
        balance.setUpdatedAt(LocalDateTime.now());
        puntosClienteRepository.save(balance);

        HistorialPuntos historial = HistorialPuntos.builder()
                .puntosCliente(balance)
                .puntos(-amount)
                .tipoAccion(accion)
                .referenciaId(referenciaId)
                .descripcion(descripcion)
                .fecha(LocalDateTime.now())
                .build();

        historialRepository.save(historial);
    }

    @Transactional(readOnly = true)
    public ClientPointsDashboardDto getClientDashboard(Long idPerfil) {
        PuntosCliente balance = puntosClienteRepository.findById(idPerfil).orElse(null);
        int total = balance != null ? balance.getPuntosTotales() : 0;

        List<PointHistoryDto> history = historialRepository
                .findByPuntosCliente_Id(idPerfil, PageRequest.of(0, 10, Sort.by("fecha").descending()))
                .stream()
                .map(h -> PointHistoryDto.builder()
                        .id(h.getId())
                        .puntos(h.getPuntos())
                        .tipoAccion(h.getTipoAccion())
                        .descripcion(h.getDescripcion())
                        .fecha(h.getFecha())
                        .build())
                .collect(Collectors.toList());

        return ClientPointsDashboardDto.builder()
                .totalPuntos(total)
                .historialReciente(history)
                .build();
    }

    private PuntosCliente getOrCreateBalance(Long idPerfil) {
        return puntosClienteRepository.findById(idPerfil)
                .orElseGet(() -> {
                    PerfilCliente perfil = clienteRepository.findById(idPerfil)
                            .orElseThrow(() -> new ResourceNotFoundException("PerfilCliente", "id", idPerfil));
                    return PuntosCliente.builder()
                            .id(idPerfil)
                            .perfilCliente(perfil)
                            .puntosTotales(0)
                            .build();
                });
    }

    private boolean isOneTimeAction(String accion) {
        return "REGISTRO".equals(accion) || "PRIMERA_COMPRA".equals(accion) || "PRIMERA_MASCOTA".equals(accion);
    }
}
