package com.vet_saas.modules.sales.dto;

import com.vet_saas.modules.sales.model.EstadoOrden;
import com.vet_saas.modules.sales.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
        private Long id;
        private String codigoOrden;
        private String clienteNombre;
        private Long empresaId;
        private String empresaNombre;
        private BigDecimal subtotal;
        private BigDecimal costoEnvio;
        private BigDecimal comisionPlataforma;
        private BigDecimal total;
        private BigDecimal descuento;
        private EstadoOrden estado;
        private String metodoPago;
        private LocalDateTime createdAt;
        private java.util.List<OrderItemResponseDto> items;

        public static OrderResponseDto fromEntity(Orden orden) {
                return OrderResponseDto.builder()
                                .id(orden.getId())
                                .codigoOrden(orden.getCodigoOrden())
                                // Si no hay perfil, usamos el correo como nombre identificador
                                .clienteNombre(orden.getUsuarioCliente().getUsername())
                                .empresaId(orden.getEmpresa() != null ? orden.getEmpresa().getId() : null)
                                .empresaNombre(orden.getEmpresa() != null ? orden.getEmpresa().getNombreComercial()
                                                : null)
                                .subtotal(orden.getSubtotal())
                                .costoEnvio(orden.getCostoEnvio())
                                .comisionPlataforma(orden.getComisionPlataforma())
                                .total(orden.getTotal())
                                .descuento(orden.getDescuento())
                                .estado(orden.getEstado())
                                .metodoPago(orden.getMetodoPago())
                                .createdAt(orden.getCreatedAt())
                                .items(orden.getDetalles().stream()
                                                .map(d -> OrderItemResponseDto.builder()
                                                                .id(d.getId())
                                                                .productoId(d.getProducto() != null
                                                                                ? d.getProducto().getId()
                                                                                : null)
                                                                .productoNombre(d.getProducto() != null
                                                                                ? d.getProducto().getNombre()
                                                                                : null)
                                                                .servicioId(d.getServicio() != null
                                                                                ? d.getServicio().getId()
                                                                                : null)
                                                                .servicioNombre(d.getServicio() != null
                                                                                ? d.getServicio().getNombre()
                                                                                : null)
                                                                .cantidad(d.getCantidad())
                                                                .precioUnitario(d.getPrecioUnitario())
                                                                .subtotal(d.getSubtotal())
                                                                .build())
                                                .toList())
                                .build();
        }
}
