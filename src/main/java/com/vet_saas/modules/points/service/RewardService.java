package com.vet_saas.modules.points.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.catalog.model.Producto;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.points.dto.CreateRewardDto;
import com.vet_saas.modules.points.dto.RedeemedRewardDto;
import com.vet_saas.modules.points.dto.RewardDto;
import com.vet_saas.modules.points.model.CanjeRecompensa;
import com.vet_saas.modules.points.model.PuntosCliente;
import com.vet_saas.modules.points.model.Recompensa;
import com.vet_saas.modules.points.repository.CanjeRecompensaRepository;
import com.vet_saas.modules.points.repository.PuntosClienteRepository;
import com.vet_saas.modules.points.repository.RecompensaRepository;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RecompensaRepository recompensaRepository;
    private final CanjeRecompensaRepository canjeRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final PuntosClienteRepository puntosClienteRepository;
    private final PointsService pointsService;
    private final OrdenRepository ordenRepository;

    @Transactional
    public RewardDto createReward(Long idEmpresa, CreateRewardDto dto) {
        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", idEmpresa));

        Recompensa recompensa = Recompensa.builder()
                .empresa(empresa)
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .costoPuntos(dto.getCostoPuntos())
                .tipoDescuento(dto.getTipoDescuento())
                .valorDescuento(dto.getValorDescuento())
                .aplicaACiertosProductos(dto.getAplicaACiertosProductos() != null ? dto.getAplicaACiertosProductos() : false)
                .activo(true)
                .build();

        if (Boolean.TRUE.equals(dto.getAplicaACiertosProductos()) && dto.getProductosAplicablesIds() != null) {
            List<Producto> productos = productoRepository.findAllById(dto.getProductosAplicablesIds());
            recompensa.setProductos(new HashSet<>(productos));
        }

        recompensa = recompensaRepository.save(recompensa);
        return mapToDto(recompensa);
    }

    @Transactional(readOnly = true)
    public Page<RewardDto> getRewardsByEmpresa(Long idEmpresa, Pageable pageable) {
        return recompensaRepository.findByEmpresa_IdAndActivoTrue(idEmpresa, pageable).map(this::mapToDto);
    }
    
    @Transactional(readOnly = true)
    public Page<RewardDto> getAllRewardsByEmpresa(Long idEmpresa, Pageable pageable) {
        return recompensaRepository.findByEmpresa_Id(idEmpresa, pageable).map(this::mapToDto);
    }

    @Transactional
    public void deactivateReward(Long idRecompensa, Long idEmpresa) {
        Recompensa recompensa = getRecompensaAndVerifyOwnership(idRecompensa, idEmpresa);
        recompensa.setActivo(false);
        recompensaRepository.save(recompensa);
    }

    @Transactional
    public RewardDto updateReward(Long idRecompensa, Long idEmpresa, CreateRewardDto dto) {
        Recompensa recompensa = getRecompensaAndVerifyOwnership(idRecompensa, idEmpresa);

        recompensa.setTitulo(dto.getTitulo());
        recompensa.setDescripcion(dto.getDescripcion());
        recompensa.setCostoPuntos(dto.getCostoPuntos());
        recompensa.setTipoDescuento(dto.getTipoDescuento());
        recompensa.setValorDescuento(dto.getValorDescuento());
        recompensa.setAplicaACiertosProductos(dto.getAplicaACiertosProductos() != null ? dto.getAplicaACiertosProductos() : false);

        if (Boolean.TRUE.equals(recompensa.getAplicaACiertosProductos()) && dto.getProductosAplicablesIds() != null) {
            List<Producto> productos = productoRepository.findAllById(dto.getProductosAplicablesIds());
            recompensa.setProductos(new HashSet<>(productos));
        } else {
            recompensa.getProductos().clear();
        }

        recompensa = recompensaRepository.save(recompensa);
        return mapToDto(recompensa);
    }

    private Recompensa getRecompensaAndVerifyOwnership(Long idRecompensa, Long idEmpresa) {
        Recompensa recompensa = recompensaRepository.findById(idRecompensa)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa", "id", idRecompensa));

        if (!recompensa.getEmpresa().getId().equals(idEmpresa)) {
            throw new ForbiddenException("No tiene permisos para modificar esta recompensa");
        }
        return recompensa;
    }

    @Transactional
    public RedeemedRewardDto redeemReward(Long idPerfil, Long idRecompensa) {
        Recompensa recompensa = recompensaRepository.findById(idRecompensa)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa", "id", idRecompensa));

        if (!recompensa.getActivo()) {
            throw new BusinessException("La recompensa no está activa");
        }

        // 1. Deduct points via PointsService (Handles validation of sufficient balance)
        pointsService.deductPoints(idPerfil, recompensa.getCostoPuntos(), "CANJE_RECOMPENSA", idRecompensa, "Canje de recompensa: " + recompensa.getTitulo());

        // 2. Create Redemption Record
        PuntosCliente cliente = puntosClienteRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("PuntosCliente", "id", idPerfil));

        CanjeRecompensa canje = CanjeRecompensa.builder()
                .puntosCliente(cliente)
                .recompensa(recompensa)
                .utilizado(false)
                .build();

        canje = canjeRepository.save(canje);
        return mapToRedeemedDto(canje);
    }

    @Transactional(readOnly = true)
    public List<RedeemedRewardDto> getAvailableRewardsForCheckout(Long idPerfil, Long idEmpresa) {
        return canjeRepository.findByPuntosCliente_IdAndUtilizadoFalseAndRecompensa_Empresa_Id(idPerfil, idEmpresa)
                .stream().map(this::mapToRedeemedDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RedeemedRewardDto> getMyRedeemedRewards(Long idPerfil, Pageable pageable) {
        return canjeRepository.findByPuntosCliente_Id(idPerfil, pageable).map(this::mapToRedeemedDto);
    }
    
    @Transactional(readOnly = true)
    public CanjeRecompensa getCanjeById(Long idCanje) {
        return canjeRepository.findById(idCanje)
                .orElseThrow(() -> new ResourceNotFoundException("CanjeRecompensa", "id", idCanje));
    }

    @Transactional
    public void markRewardAsUsed(Long idCanje, Long ordenId) {
        CanjeRecompensa canje = canjeRepository.findById(idCanje)
                .orElseThrow(() -> new ResourceNotFoundException("CanjeRecompensa", "id", idCanje));
                
        canje.setUtilizado(true);
        canje.setFechaUtilizacion(LocalDateTime.now());
        
        if (ordenId != null) {
            Orden orden = ordenRepository.findById(ordenId)
                    .orElseThrow(() -> new ResourceNotFoundException("Orden", "id", ordenId));
            canje.setOrden(orden);
        }
        
        canjeRepository.save(canje);
    }

    @Transactional
    public void linkCanjeToOrder(Long idCanje, Long ordenId) {
        canjeRepository.findById(idCanje).ifPresent(canje -> {
            canje.setOrden(ordenRepository.findById(ordenId).orElse(null));
            canjeRepository.save(canje);
        });
    }

    private RewardDto mapToDto(Recompensa entity) {
        return RewardDto.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa().getId())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .costoPuntos(entity.getCostoPuntos())
                .tipoDescuento(entity.getTipoDescuento())
                .valorDescuento(entity.getValorDescuento())
                .aplicaACiertosProductos(entity.getAplicaACiertosProductos())
                .activo(entity.getActivo())
                .totalCanjes((int) canjeRepository.countByRecompensa_Id(entity.getId()))
                .productosAplicablesIds(entity.getProductos() != null ? 
                    entity.getProductos().stream().map(Producto::getId).collect(Collectors.toList()) : null)
                .build();
    }

    private RedeemedRewardDto mapToRedeemedDto(CanjeRecompensa entity) {
        return RedeemedRewardDto.builder()
                .id(entity.getId())
                .recompensaId(entity.getRecompensa().getId())
                .recompensaTitulo(entity.getRecompensa().getTitulo())
                .tipoDescuento(entity.getRecompensa().getTipoDescuento())
                .valorDescuento(entity.getRecompensa().getValorDescuento())
                .fechaCanje(entity.getFechaCanje())
                .utilizado(entity.getUtilizado())
                .fechaUtilizacion(entity.getFechaUtilizacion())
                .ordenId(entity.getOrden() != null ? entity.getOrden().getId() : null)
                .build();
    }
}
