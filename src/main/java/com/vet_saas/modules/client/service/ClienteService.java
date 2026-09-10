package com.vet_saas.modules.client.service;

import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.client.dto.ClienteCrmResponse;
import com.vet_saas.modules.client.dto.ClienteResponse;
import com.vet_saas.modules.client.dto.CreateClienteDto;
import com.vet_saas.modules.client.dto.UpdateClienteDto;
import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.user.model.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmpresaLookupService empresaLookupService;
    private final PointsService pointsService;
    private final AppProperties appProperties;
    private final OrdenRepository ordenRepository;
    private final CitaRepository citaRepository;

    @Transactional
    public ClienteResponse createPerfil(Usuario usuario, CreateClienteDto dto) {
        if (clienteRepository.existsByUsuarioIdAndActivoTrue(usuario.getId())) {
            throw new BusinessException("Ya tienes un perfil de cliente registrado.");
        }

        PerfilCliente perfil = PerfilCliente.builder()
                .usuario(usuario)
                .nombres(dto.nombres())
                .apellidos(dto.apellidos())
                .telefono(dto.telefono())
                .direccion(dto.direccion())
                .ciudad(dto.ciudad())
                .pais(dto.pais() != null ? dto.pais() : appProperties.getBusiness().getDefaultCountry())
                .activo(true)
                .build();

        perfil = clienteRepository.save(perfil);

        try {
            pointsService.addPoints(perfil.getId(), "REGISTRO", null, "Bono de bienvenida por registro");
        } catch (Exception e) {
            log.error("Error granting registration points: {}", e.getMessage());
        }

        return mapToResponse(perfil);
    }

    @Transactional(readOnly = true)
    public ClienteResponse getMyPerfil(Usuario usuario) {
        PerfilCliente perfil = clienteRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PerfilCliente", "usuarioId", usuario.getId()));
        return mapToResponse(perfil);
    }

    @Transactional
    public ClienteResponse updateMyPerfil(Usuario usuario, UpdateClienteDto dto, String fotoUrl) {
        PerfilCliente perfil = clienteRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PerfilCliente", "usuarioId", usuario.getId()));

        if (dto.nombres() != null) perfil.setNombres(dto.nombres());
        if (dto.apellidos() != null) perfil.setApellidos(dto.apellidos());
        if (dto.telefono() != null) perfil.setTelefono(dto.telefono());
        if (dto.direccion() != null) perfil.setDireccion(dto.direccion());
        if (dto.ciudad() != null) perfil.setCiudad(dto.ciudad());
        if (dto.pais() != null) perfil.setPais(dto.pais());
        if (fotoUrl != null) perfil.setFotoPerfilUrl(fotoUrl);

        return mapToResponse(clienteRepository.save(perfil));
    }

    @Transactional
    public void deleteMiPerfil(Usuario usuario) {
        PerfilCliente perfil = clienteRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PerfilCliente", "usuarioId", usuario.getId()));
        perfil.setActivo(false);
        clienteRepository.save(perfil);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> getClientesByEmpresa(Usuario usuario, String q, Pageable pageable) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        return clienteRepository.findClientesByEmpresaId(empresa.getId(), q, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClienteCrmResponse> getClientesCrmByEmpresa(Usuario usuario, String q, Pageable pageable) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);

        Map<Long, Object[]> spendByUsuarioId = new HashMap<>();
        for (Object[] row : ordenRepository.findSpendSummaryByEmpresa(empresa.getId())) {
            spendByUsuarioId.put((Long) row[0], row);
        }

        Map<Long, Object[]> citaSummaryByUsuarioId = new HashMap<>();
        for (Object[] row : citaRepository.findCitaSummaryByEmpresa(empresa.getId())) {
            citaSummaryByUsuarioId.put((Long) row[0], row);
        }

        return clienteRepository.findClientesByEmpresaId(empresa.getId(), q, pageable)
                .map(perfil -> {
                    Long usuarioId = perfil.getUsuario().getId();
                    Object[] spend = spendByUsuarioId.get(usuarioId);
                    Object[] citaSummary = citaSummaryByUsuarioId.get(usuarioId);

                    BigDecimal gastoOrdenes = spend != null ? (BigDecimal) spend[1] : BigDecimal.ZERO;
                    Long totalPedidos = spend != null ? (Long) spend[2] : 0L;
                    LocalDateTime ultimaOrden = spend != null ? (LocalDateTime) spend[3] : null;

                    Long totalCitas = citaSummary != null ? (Long) citaSummary[1] : 0L;
                    BigDecimal gastoCitas = citaSummary != null ? (BigDecimal) citaSummary[2] : BigDecimal.ZERO;
                    LocalDate ultimaCita = citaSummary != null ? (LocalDate) citaSummary[3] : null;

                    BigDecimal totalGastado = gastoOrdenes.add(gastoCitas);
                    LocalDateTime ultimaCitaDateTime = ultimaCita != null ? ultimaCita.atStartOfDay() : null;
                    LocalDateTime ultimaCompra = maxNullable(ultimaOrden, ultimaCitaDateTime);

                    return new ClienteCrmResponse(
                            perfil.getId(),
                            usuarioId,
                            perfil.getUsuario().getCorreo(),
                            perfil.getNombres(),
                            perfil.getApellidos(),
                            perfil.getTelefono(),
                            perfil.getFotoPerfilUrl(),
                            totalGastado,
                            totalPedidos,
                            totalCitas,
                            ultimaCompra);
                });
    }

    @Transactional(readOnly = true)
    public ClienteResponse getClienteByIdForEmpresa(Usuario usuario, Long perfilId) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);

        PerfilCliente perfil = clienteRepository.findById(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", perfilId));

        boolean esClienteDeLaEmpresa = clienteRepository.existsByEmpresaIdAndClienteId(empresa.getId(), perfilId);

        if (!esClienteDeLaEmpresa) {
            throw new ForbiddenException("No tienes acceso al perfil de este cliente.");
        }

        return mapToResponse(perfil);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> searchClientes(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return clienteRepository.findAll(pageable).map(this::mapToResponse);
        }
        return clienteRepository.searchByNombre(q, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponse getClienteById(Long id) {
        return clienteRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }

    private LocalDateTime maxNullable(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private ClienteResponse mapToResponse(PerfilCliente perfil) {
        return new ClienteResponse(
                perfil.getId(),
                perfil.getUsuario().getId(),
                perfil.getUsuario().getCorreo(),
                perfil.getNombres(),
                perfil.getApellidos(),
                perfil.getTelefono(),
                perfil.getDireccion(),
                perfil.getCiudad(),
                perfil.getPais(),
                perfil.getFotoPerfilUrl(),
                perfil.getUbicacionLat(),
                perfil.getUbicacionLng(),
                perfil.getUpdatedAt());
    }
}
