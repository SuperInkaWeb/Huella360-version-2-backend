package com.vet_saas.modules.dashboard.service;

import com.vet_saas.modules.appointment.model.AppointmentStatus;
import com.vet_saas.modules.appointment.model.Cita;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.dashboard.dto.*;
import com.vet_saas.modules.sales.model.EstadoOrden;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.DetalleOrdenRepository;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdenRepository ordenRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final CitaRepository citaRepository;
    private final EmpresaRepository empresaRepository;

    @Cacheable(value = "empresasByPropietario", key = "#usuarioId")
    public Long resolveEmpresaId(Long usuarioId) {
        return empresaRepository.findByUsuarioPropietarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Empresa no encontrada para el propietario actual"))
                .getId();
    }

    @Cacheable(value = "dashboardMetrics", key = "#empresaId")
    @Transactional(readOnly = true)
    public DashboardMetricsDto getMetrics(Long empresaId) {

        LocalDateTime inicioDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime inicioMes = LocalDateTime.now().minusDays(30);
        LocalDateTime inicioSemana = LocalDateTime.now().minusDays(7);
        LocalDateTime mesAnteriorInicio = LocalDateTime.now().minusDays(60);
        LocalDateTime mesAnteriorFin = LocalDateTime.now().minusDays(30);

        BigDecimal totalVentasMes = ordenRepository.sumTotalByEmpresaAndEstadoAndFechaGte(
                empresaId, EstadoOrden.PAGADO, inicioMes);

        BigDecimal ventasSemana = ordenRepository.sumTotalByEmpresaAndEstadoAndFechaGte(
                empresaId, EstadoOrden.PAGADO, inicioSemana);

        BigDecimal ventasMesAnterior = ordenRepository.sumTotalByEmpresaAndEstadoAndFechaGte(
                empresaId, EstadoOrden.PAGADO, mesAnteriorInicio);

        Long ordenesPendientes = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.PENDIENTE);

        Long ordenesPagadasHoy = ordenRepository.countByEmpresaEstadoAndFechaGte(
                empresaId, EstadoOrden.PAGADO, inicioDia);

        Long clientesActivos = ordenRepository.countDistinctClientesByEmpresa(empresaId);

        long citasHoy = citaRepository.countByEmpresaIdAndFecha(empresaId, LocalDate.now());
        long citasPendientes = citaRepository.countByEmpresaIdAndEstado(empresaId, AppointmentStatus.SOLICITADA);

        List<TopProductoDto> topProductos = detalleOrdenRepository.findTopProductos(
                empresaId, PageRequest.of(0, 5));

        List<TopServicioDto> topServicios = citaRepository.findTopServiciosByEmpresa(
                empresaId, PageRequest.of(0, 5));

        return DashboardMetricsDto.builder()
                .totalVentasMes(totalVentasMes)
                .ventasSemana(ventasSemana)
                .ventasMesAnterior(ventasMesAnterior)
                .ordenesPendientes(ordenesPendientes)
                .ordenesPagadasHoy(ordenesPagadasHoy)
                .clientesActivos(clientesActivos)
                .citasHoy(citasHoy)
                .citasPendientes(citasPendientes)
                .topProductos(topProductos)
                .topServicios(topServicios)
                .build();
    }

    @Cacheable(value = "dashboardChart", key = "#empresaId")
    @Transactional(readOnly = true)
    public List<VentaDiariaDto> getChartData(Long empresaId) {
        LocalDateTime inicio30Dias = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = ordenRepository.findVentasDiariasRaw(empresaId, inicio30Dias);
        return rows.stream()
                .map(row -> VentaDiariaDto.builder()
                        .fecha(((java.sql.Date) row[0]).toLocalDate())
                        .total((BigDecimal) row[1])
                        .cantidadOrdenes((Long) row[2])
                        .build())
                .toList();
    }

    @Cacheable(value = "dashboardActivity", key = "#empresaId")
    @Transactional(readOnly = true)
    public List<ActividadRecienteDto> getRecentActivity(Long empresaId) {
        List<ActividadRecienteDto> actividad = new ArrayList<>();

        List<Orden> ordenesRecientes = ordenRepository.findRecentByEmpresa(empresaId, PageRequest.of(0, 5));
        for (Orden orden : ordenesRecientes) {
            String clienteNombre = orden.getGuestNombre();
            if (clienteNombre == null && orden.getUsuarioCliente() != null) {
                clienteNombre = orden.getUsuarioCliente().getCorreo();
            }
            actividad.add(ActividadRecienteDto.builder()
                    .id(orden.getId())
                    .tipo("ORDEN")
                    .descripcion("Orden " + orden.getCodigoOrden())
                    .clienteNombre(clienteNombre != null ? clienteNombre : "Cliente")
                    .monto(orden.getTotal())
                    .estado(orden.getEstado().name())
                    .fecha(orden.getCreatedAt())
                    .build());
        }

        List<Cita> citasRecientes = citaRepository.findRecentByEmpresa(empresaId, PageRequest.of(0, 5));
        for (Cita cita : citasRecientes) {
            String clienteNombre = null;
            if (cita.getCliente() != null) {
                clienteNombre = cita.getCliente().getCorreo();
            }
            String servicioNombre = cita.getServicio() != null ? cita.getServicio().getNombre() : "Sin servicio";
            actividad.add(ActividadRecienteDto.builder()
                    .id(cita.getId())
                    .tipo("CITA")
                    .descripcion("Cita: " + servicioNombre)
                    .clienteNombre(clienteNombre != null ? clienteNombre : "Cliente")
                    .monto(null)
                    .estado(cita.getEstado().name())
                    .fecha(cita.getCreatedAt())
                    .build());
        }

        actividad.sort((a, b) -> {
            if (a.getFecha() == null && b.getFecha() == null) return 0;
            if (a.getFecha() == null) return 1;
            if (b.getFecha() == null) return -1;
            return b.getFecha().compareTo(a.getFecha());
        });

        return actividad.stream().limit(10).collect(Collectors.toList());
    }
}
