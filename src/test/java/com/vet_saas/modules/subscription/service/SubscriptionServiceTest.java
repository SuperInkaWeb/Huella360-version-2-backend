package com.vet_saas.modules.subscription.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.ia.repository.IaUsageRepository;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import com.vet_saas.modules.subscription.model.EstadoSuscripcion;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import com.vet_saas.modules.subscription.repository.PlanRepository;
import com.vet_saas.modules.subscription.repository.SuscripcionRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H360-SEC: regresion para el bug donde CLIENTE/EMPRESA/VETERINARIO podian llamar
 * PATCH /subscriptions/update-plan?planId=X directamente y quedar en un plan de pago
 * (con todos sus limites) sin pasar por /subscriptions/checkout/{planId} -> Mercado Pago
 * -> webhook, es decir, sin pagar nada. Confirmado en vivo el 2026-09-22 contra el backend
 * local: una empresa con el plan gratuito paso al plan "Negocio Starter" (S/49.00) al
 * instante, con estado ACTIVA y sin ningun cargo.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private EmpresaLookupService empresaLookupService;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private MercadoPagoGateway mercadoPagoGateway;
    @Mock private com.vet_saas.config.AppProperties appProperties;
    @Mock private IaUsageRepository iaUsageRepository;

    private SubscriptionService subscriptionService;

    private Plan planGratuito;
    private Plan planPago;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                suscripcionRepository, planRepository, productoRepository, empresaLookupService,
                veterinarioRepository, mascotaRepository, servicioRepository, mercadoPagoGateway,
                appProperties, iaUsageRepository);

        planGratuito = Plan.builder()
                .id(7L).nombre("Huella Free B2B").precioMensual(BigDecimal.ZERO)
                .limiteProductos(4).limiteServicios(4).tipo("B2B").activo(true).build();

        planPago = Plan.builder()
                .id(8L).nombre("Negocio Starter").precioMensual(new BigDecimal("49.00"))
                .limiteProductos(30).limiteServicios(30).tipo("B2B").activo(true).build();
    }

    @Test
    void updatePlanForUsuario_cliente_rechazaPlanDePago() {
        Usuario cliente = Usuario.builder().id(1L).correo("cliente@test.com").rol(Role.CLIENTE).build();
        Suscripcion subActual = Suscripcion.builder().id(10L).plan(planGratuito).estado(EstadoSuscripcion.ACTIVA).build();

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(subActual));
        when(planRepository.findById(8L)).thenReturn(Optional.of(planPago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> subscriptionService.updatePlanForUsuario(cliente, 8L));
        assertTrue(ex.getMessage().contains("pago"));
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    void updatePlanForUsuario_cliente_permitePlanGratuito() {
        Usuario cliente = Usuario.builder().id(1L).correo("cliente@test.com").rol(Role.CLIENTE).build();
        Suscripcion subActual = Suscripcion.builder().id(10L).plan(planPago).estado(EstadoSuscripcion.ACTIVA).build();

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(subActual));
        when(planRepository.findById(7L)).thenReturn(Optional.of(planGratuito));
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = subscriptionService.updatePlanForUsuario(cliente, 7L);

        assertNotNull(response);
        verify(suscripcionRepository).save(any());
    }

    @Test
    void updatePlanForUsuario_empresa_rechazaPlanDePago() {
        Usuario usuarioEmpresa = Usuario.builder().id(2L).correo("empresa@test.com").rol(Role.EMPRESA).build();
        Empresa empresa = Empresa.builder().id(5L).nombreComercial("Empresa Test").build();
        Suscripcion subActual = Suscripcion.builder().id(11L).empresa(empresa).plan(planGratuito).estado(EstadoSuscripcion.ACTIVA).build();

        when(empresaLookupService.getEmpresaFromUsuario(usuarioEmpresa)).thenReturn(empresa);
        when(suscripcionRepository.findByEmpresaId(5L)).thenReturn(Optional.of(subActual));
        when(planRepository.findById(8L)).thenReturn(Optional.of(planPago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> subscriptionService.updatePlanForUsuario(usuarioEmpresa, 8L));
        assertTrue(ex.getMessage().contains("pago"));
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    void updatePlanForUsuario_empresa_permitePlanGratuito() {
        Usuario usuarioEmpresa = Usuario.builder().id(2L).correo("empresa@test.com").rol(Role.EMPRESA).build();
        Empresa empresa = Empresa.builder().id(5L).nombreComercial("Empresa Test").build();
        Suscripcion subActual = Suscripcion.builder().id(11L).empresa(empresa).plan(planPago).estado(EstadoSuscripcion.ACTIVA).build();

        when(empresaLookupService.getEmpresaFromUsuario(usuarioEmpresa)).thenReturn(empresa);
        when(suscripcionRepository.findByEmpresaId(5L)).thenReturn(Optional.of(subActual));
        when(planRepository.findById(7L)).thenReturn(Optional.of(planGratuito));
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = subscriptionService.updatePlanForUsuario(usuarioEmpresa, 7L);

        assertNotNull(response);
        verify(suscripcionRepository).save(any());
    }
}
