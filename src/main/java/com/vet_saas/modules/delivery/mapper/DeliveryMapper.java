package com.vet_saas.modules.delivery.mapper;

import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryEmpresaResponseDTO;
import com.vet_saas.modules.delivery.dto.response.RepartidorResponseDTO;
import com.vet_saas.modules.delivery.model.Delivery;
import com.vet_saas.modules.delivery.model.Repartidor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryMapper {

    private final ClienteRepository clienteRepository;

    public DeliveryResponseDTO toResponseDTO(Delivery delivery) {
        if (delivery == null) return null;
        PerfilCliente cliente = getCliente(delivery);

        return DeliveryResponseDTO.builder()
                .idDelivery(delivery.getId())
                .ordenId(delivery.getOrden() != null ? delivery.getOrden().getId() : null)
                .empresaId(delivery.getOrden() != null && delivery.getOrden().getEmpresa() != null
                        ? delivery.getOrden().getEmpresa().getId() : null)
                .estado(delivery.getEstado())
                .clienteNombre(cliente != null ? cliente.getNombres() + " " + cliente.getApellidos() : "Cliente")
                .clienteTelefono(cliente != null ? cliente.getTelefono() : null)
                .repartidorId(delivery.getRepartidor() != null ? delivery.getRepartidor().getIdRepartidor() : null)
                .repartidorNombre(getRepartidorNombre(delivery))
                .repartidorFoto(delivery.getRepartidor() != null ? delivery.getRepartidor().getFotoPerfil() : null)
                .repartidorTelefono(delivery.getRepartidor() != null ? delivery.getRepartidor().getTelefono() : null)
                .repartidorVehiculo(delivery.getRepartidor() != null && delivery.getRepartidor().getTipoVehiculo() != null
                        ? delivery.getRepartidor().getTipoVehiculo().name() : null)
                .repartidorLat(delivery.getRepartidor() != null ? delivery.getRepartidor().getUbicacionLat() : null)
                .repartidorLng(delivery.getRepartidor() != null ? delivery.getRepartidor().getUbicacionLng() : null)
                .repartidorCalificacionPromedio(delivery.getRepartidor() != null ? delivery.getRepartidor().getCalificacionPromedio() : null)
                .destinoLat(delivery.getDestinoLat())
                .destinoLng(delivery.getDestinoLng())
                .destinoDireccion(delivery.getDestinoDireccion())
                .destinoReferencia(delivery.getDestinoReferencia())
                .origenLat(delivery.getOrigenLat())
                .origenLng(delivery.getOrigenLng())
                .origenDireccion(delivery.getOrigenDireccion())
                .tiempoEstimadoMin(delivery.getTiempoEstimadoMin())
                .distanciaKm(delivery.getDistanciaKm())
                .costoDelivery(delivery.getCostoDelivery())
                .otpCliente(null)
                .calificacionRepartidor(delivery.getCalificacionRepartidor())
                .comentarioRepartidor(null)
                .calificacionProducto(delivery.getCalificacionProducto())
                .comentarioProducto(delivery.getComentarioProducto())
                .fotoEntregaUrl(delivery.getFotoEntregaUrl())
                .asignadoAt(delivery.getAsignadoAt())
                .recogidoAt(delivery.getRecogidoAt())
                .entregadoAt(delivery.getEntregadoAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    public DeliveryResponseDTO toResponseDTOConOTP(Delivery delivery, String otp) {
        DeliveryResponseDTO dto = toResponseDTO(delivery);
        if (dto != null) {
            dto.setOtpCliente(otp);
            dto.setRepartidorVehiculo(null);
        }
        return dto;
    }

    public RepartidorResponseDTO toRepartidorDTO(Repartidor repartidor) {
        if (repartidor == null) return null;

        return RepartidorResponseDTO.builder()
                .idRepartidor(repartidor.getIdRepartidor())
                .nombres(repartidor.getNombres())
                .apellidos(repartidor.getApellidos())
                .telefono(repartidor.getTelefono())
                .fotoPerfil(repartidor.getFotoPerfil())
                .tipoVehiculo(repartidor.getTipoVehiculo())
                .placaVehiculo(repartidor.getPlacaVehiculo())
                .estadoActual(repartidor.getEstadoActual())
                .estadoValidacion(repartidor.getEstadoValidacion())
                .calificacionPromedio(repartidor.getCalificacionPromedio())
                .totalEntregas(repartidor.getTotalEntregas())
                .ubicacionLat(repartidor.getUbicacionLat())
                .ubicacionLng(repartidor.getUbicacionLng())
                .ultimaUbicacionAt(repartidor.getUltimaUbicacionAt())
                .activo(repartidor.getActivo())
                .build();
    }

    public DeliveryEmpresaResponseDTO toEmpresaResponseDTO(Delivery delivery) {
        PerfilCliente cliente = getCliente(delivery);
        Repartidor repartidor = delivery.getRepartidor();
        List<String> productos = delivery.getOrden().getDetalles().stream()
            .map(detalle -> detalle.getCantidad() + " x " +
                (detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Producto"))
            .toList();
        int totalUnidades = delivery.getOrden().getDetalles().stream()
            .mapToInt(detalle -> detalle.getCantidad() != null ? detalle.getCantidad() : 0)
            .sum();

        return DeliveryEmpresaResponseDTO.builder()
            .idDelivery(delivery.getId())
            .ordenId(delivery.getOrden().getId())
            .codigoOrden(delivery.getOrden().getCodigoOrden())
            .estado(delivery.getEstado())
            .clienteNombre(cliente != null ? cliente.getNombres() + " " + cliente.getApellidos()
                : delivery.getOrden().getGuestNombre())
            .clienteTelefono(cliente != null ? cliente.getTelefono() : null)
            .totalUnidades(totalUnidades)
            .productos(productos)
            .repartidorId(repartidor != null ? repartidor.getIdRepartidor() : null)
            .repartidorNombre(repartidor != null ? repartidor.getNombres() + " " + repartidor.getApellidos() : null)
            .repartidorTelefono(repartidor != null ? repartidor.getTelefono() : null)
            .repartidorVehiculo(repartidor != null && repartidor.getTipoVehiculo() != null ? repartidor.getTipoVehiculo().name() : null)
            .repartidorPlaca(repartidor != null ? repartidor.getPlacaVehiculo() : null)
            .repartidorLat(repartidor != null ? repartidor.getUbicacionLat() : null)
            .repartidorLng(repartidor != null ? repartidor.getUbicacionLng() : null)
            .origenLat(delivery.getOrigenLat()).origenLng(delivery.getOrigenLng()).origenDireccion(delivery.getOrigenDireccion())
            .destinoLat(delivery.getDestinoLat()).destinoLng(delivery.getDestinoLng()).destinoDireccion(delivery.getDestinoDireccion())
            .destinoReferencia(delivery.getDestinoReferencia())
            .fotoEntregaUrl(delivery.getFotoEntregaUrl())
            .createdAt(delivery.getCreatedAt()).asignadoAt(delivery.getAsignadoAt()).entregadoAt(delivery.getEntregadoAt())
            .empresaConfirmadoAt(delivery.getEmpresaConfirmadoAt())
            .build();
    }

    private String getRepartidorNombre(Delivery delivery) {
        if (delivery.getRepartidor() == null) return null;
        return delivery.getRepartidor().getNombres() + " " + delivery.getRepartidor().getApellidos();
    }

    private PerfilCliente getCliente(Delivery delivery) {
        if (delivery.getOrden() == null || delivery.getOrden().getUsuarioCliente() == null) return null;
        return clienteRepository.findByUsuarioId(delivery.getOrden().getUsuarioCliente().getId())
                .orElse(null);
    }
}
