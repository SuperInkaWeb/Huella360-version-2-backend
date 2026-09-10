package com.vet_saas.modules.delivery.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.delivery.dto.request.CalificacionDTO;
import com.vet_saas.modules.delivery.dto.request.CambiarEstadoDTO;
import com.vet_saas.modules.delivery.dto.request.ConfirmarOTPDTO;
import com.vet_saas.modules.delivery.dto.request.ReportarIncidenciaDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryResponseDTO;
import com.vet_saas.modules.delivery.dto.response.DeliveryEmpresaResponseDTO;
import com.vet_saas.modules.delivery.service.DeliveryService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ---- Cliente ----

    /** Consultar el delivery de una orden (cliente ve estado + posicion del repartidor) */
    @GetMapping("/orden/{ordenId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> getByOrden(
            @PathVariable Long ordenId) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getByOrdenId(ordenId), "Delivery de la orden"));
    }

    /** El cliente califica al repartidor tras recibir su pedido */
    @PostMapping("/{deliveryId}/calificar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> calificar(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CalificacionDTO dto,
            @AuthenticationPrincipal Usuario principal) {
        deliveryService.calificarEntrega(deliveryId, dto, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Calificación registrada"));
    }

    /** El cliente cancela su pedido (solo si no ha sido recogido) */
    @PostMapping("/{deliveryId}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> cancelar(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.cancelarDelivery(deliveryId, principal.getId()), "Delivery cancelado"));
    }

    /** El cliente vuelve a publicar un envío fallido o con incidencia en el pool. */
    @PostMapping("/{deliveryId}/reintentar-cliente")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> reintentarComoCliente(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.reintentarDeliveryCliente(deliveryId, principal.getId()),
            "Se está buscando un nuevo repartidor"
        ));
    }

    /** Genera un PIN nuevo para el cliente e invalida inmediatamente el anterior. */
    @PostMapping("/{deliveryId}/actualizar-pin")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> actualizarPin(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        String pin = deliveryService.regenerarOTP(deliveryId, principal.getId());
        java.util.Map<String, String> data = java.util.Map.of("pin", pin);
        return ResponseEntity.ok(ApiResponse.success(data, "PIN generado correctamente"));
    }

    // ---- Repartidor ----

    /** El repartidor cambia el estado del delivery (EN_TIENDA, RECOGIDO, EN_CAMINO, etc.) */
    @PatchMapping("/{deliveryId}/estado")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> cambiarEstado(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CambiarEstadoDTO dto,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.cambiarEstado(deliveryId, dto.getNuevoEstado(),
                principal.getId(), dto.getDescripcion()), "Estado actualizado")
        );
    }

    /** Confirmar entrega con OTP que el cliente le dice al repartidor */
    @PostMapping("/{deliveryId}/confirmar-otp")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> confirmarOTP(
            @PathVariable Long deliveryId,
            @Valid @RequestBody ConfirmarOTPDTO dto,
            @AuthenticationPrincipal Usuario principal) {
        deliveryService.confirmarEntregaOTP(deliveryId, dto, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Entrega confirmada con OTP"));
    }

    /** Confirmar entrega con foto (cuando nadie abre o como evidencia adicional) */
    @PostMapping("/{deliveryId}/confirmar-foto")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> confirmarFoto(
            @PathVariable Long deliveryId,
            @RequestParam("foto") MultipartFile foto,
            @AuthenticationPrincipal Usuario principal) {
        deliveryService.confirmarEntregaFoto(deliveryId, foto, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Evidencia fotográfica guardada"));
    }

    /** Reportar intento fallido (nadie abre la puerta) */
    @PostMapping("/{deliveryId}/intento-fallido")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> intentoFallido(
            @PathVariable Long deliveryId,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            @RequestParam(value = "motivo", required = false) String motivo,
            @AuthenticationPrincipal Usuario principal) {
        deliveryService.reportarIntentoFallido(deliveryId, foto, motivo, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Intento fallido reportado"));
    }

    /** Reportar incidencia (Accidente, Robo, Falla Mecánica) */
    @PostMapping(value = "/{deliveryId}/incidencia", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<Void>> reportarIncidencia(
            @PathVariable Long deliveryId,
            @RequestPart("data") @Valid ReportarIncidenciaDTO dto,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @AuthenticationPrincipal Usuario principal) {
        deliveryService.reportarIncidencia(deliveryId, dto.getMotivo(), dto.getDescripcion(), foto, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Incidencia reportada correctamente"));
    }

    /** Reiniciar un delivery que falló o tuvo incidencia (Solo Empresa/Admin) */
    @PostMapping("/{deliveryId}/reintentar")
    @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> reintentar(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.reintentarDelivery(deliveryId, usuario.getId()), 
            "Delivery reiniciado correctamente")
        );
    }

    /** Reiniciar un delivery por ID de orden (Solo Empresa/Admin) */
    @PostMapping("/orden/{ordenId}/reintentar")
    @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> reintentarByOrder(
            @PathVariable Long ordenId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.reintentarDeliveryByOrder(ordenId, usuario.getId()), 
            "Delivery reiniciado correctamente")
        );
    }

    /** Repartidor ve el pool de pedidos disponibles */
    @GetMapping("/disponibles")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<java.util.List<DeliveryResponseDTO>>> getDisponibles() {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getPedidosDisponibles(), "Pedidos disponibles"));
    }

    /** Repartidor toma un pedido del pool */
    @PostMapping("/{deliveryId}/aceptar")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> aceptarPedido(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.aceptarPedido(deliveryId, principal.getId()), "Pedido aceptado"));
    }

    /** Repartidor rechaza un pedido que tenía asignado (vuelve al pool) */
    @PostMapping("/{deliveryId}/rechazar")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> rechazarPedido(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.rechazarPedido(deliveryId, principal.getId()),
            "Pedido rechazado. Vuelve al pool de disponibles.")
        );
    }

    // ---- Empresa (dashboard) ----

    /** Historial y seguimiento de los envíos pertenecientes a la empresa autenticada. */
    @GetMapping("/empresa/seguimiento")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<java.util.List<DeliveryEmpresaResponseDTO>>> seguimientoEmpresa(
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.getSeguimientoEmpresa(principal.getId()), "Seguimiento de deliveries"));
    }

    /** La empresa confirma una entrega y cierra el seguimiento GPS de ese pedido. */
    @PostMapping("/{deliveryId}/confirmar-empresa")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<DeliveryEmpresaResponseDTO>> confirmarEntregaEmpresa(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(
            deliveryService.confirmarEntregaEmpresa(deliveryId, principal.getId()),
            "Entrega confirmada por la empresa"));
    }

    /** Ver detalle de un delivery específico */
    @GetMapping("/{deliveryId}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'ADMIN', 'REPARTIDOR')")
    public ResponseEntity<ApiResponse<DeliveryResponseDTO>> getById(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getById(deliveryId), "Detalle del delivery"));
    }

    /** Ver calificaciones recibidas por la empresa */
    @GetMapping("/empresa/ratings")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<java.util.List<DeliveryResponseDTO>>> getRatingsByEmpresa(
            @AuthenticationPrincipal Usuario principal) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getRatingsByUsuarioEmpresa(principal.getId()), "Calificaciones de la empresa"));
    }
}
