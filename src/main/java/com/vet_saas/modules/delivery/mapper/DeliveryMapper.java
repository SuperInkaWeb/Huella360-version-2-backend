package com.vet_saas.modules.delivery.mapper;

import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.RepartidorResponseDTO;
import com.vet_saas.modules.delivery.model.Delivery;
import com.vet_saas.modules.delivery.model.Repartidor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryMapper {

    private final ClienteRepository clienteRepository;

    public DeliveryResponseDTO toResponseDTO(Delivery delivery) {
        if (delivery == null) return null;

        return DeliveryResponseDTO.builder()
                .idDelivery(delivery.getId())
                .ordenId(delivery.getOrden() != null ? delivery.getOrden().getId() : null)
                .estado(delivery.getEstado())
                .clienteNombre(getClienteNombre(delivery))
                .clienteTelefono(getClienteTelefono(delivery))
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

    private String getRepartidorNombre(Delivery delivery) {
        if (delivery.getRepartidor() == null) return null;
        return delivery.getRepartidor().getNombres() + " " + delivery.getRepartidor().getApellidos();
    }

    private String getClienteNombre(Delivery delivery) {
        if (delivery.getOrden() == null || delivery.getOrden().getUsuarioCliente() == null) return "Cliente";
        return clienteRepository.findByUsuarioId(delivery.getOrden().getUsuarioCliente().getId())
                .map(p -> p.getNombres() + " " + p.getApellidos())
                .orElse("Cliente");
    }

    private String getClienteTelefono(Delivery delivery) {
        if (delivery.getOrden() == null || delivery.getOrden().getUsuarioCliente() == null) return null;
        return clienteRepository.findByUsuarioId(delivery.getOrden().getUsuarioCliente().getId())
                .map(PerfilCliente::getTelefono)
                .orElse(null);
    }
}
