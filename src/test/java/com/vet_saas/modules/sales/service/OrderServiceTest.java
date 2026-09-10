package com.vet_saas.modules.sales.service;

import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.catalog.model.Producto;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.catalog.repository.ServicioRepository;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.points.service.RewardService;
import com.vet_saas.modules.sales.dto.CreateOrderDto;
import com.vet_saas.modules.sales.dto.OrderItemDto;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PointsService pointsService;
    @Mock private ClienteRepository clienteRepository;
    @Mock private RewardService rewardService;
    @Mock private AppProperties appProperties;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        AppProperties.Business businessProps = new AppProperties.Business();
        lenient().when(appProperties.getBusiness()).thenReturn(businessProps);

        orderService = new OrderService(
                ordenRepository, productoRepository, servicioRepository,
                empresaRepository, veterinarioRepository, usuarioRepository,
                pointsService, clienteRepository, rewardService, appProperties);
        SecurityContextHolder.clearContext();
    }

    private Usuario buildUser(Long id, Role role) {
        Usuario user = Usuario.builder()
                .id(id)
                .correo("test@test.com")
                .password("encoded")
                .rol(role)
                .estado(true)
                .build();
        return user;
    }

    private Empresa buildEmpresa(Long id, Usuario owner) {
        return Empresa.builder()
                .id(id)
                .usuarioPropietario(owner)
                .nombreComercial("Pet Shop")
                .build();
    }

    @Test
    void createOrder_noEmpresaNoVet_throwsBusinessException() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(user));

        CreateOrderDto dto = new CreateOrderDto(null, null, null, null, null, null, null, null, null, null,
                List.of(new OrderItemDto(1L, null, 2)));

        assertThrows(BusinessException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void createOrder_withEmpresa_succeeds() throws Exception {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Empresa empresa = buildEmpresa(10L, user);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(user));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));

        Producto producto = Producto.builder()
                .id(100L)
                .nombre("Shampoo")
                .precio(new BigDecimal("25.00"))
                .stock(50)
                .empresa(empresa)
                .build();
        when(productoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(producto));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        lenient().when(ordenRepository.findByUsuarioClienteId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        CreateOrderDto dto = new CreateOrderDto(10L, null, null, null, null, null, null, null, null, null,
                List.of(new OrderItemDto(100L, null, 2)));

        Long orderId = orderService.createOrder(dto);

        assertNotNull(orderId);
        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenRepository).save(captor.capture());
        Orden saved = captor.getValue();
        assertEquals(0, new BigDecimal("50.00").compareTo(saved.getSubtotal()));
        assertEquals(0, new BigDecimal("2.50").compareTo(saved.getComisionPlataforma()));
    }

    @Test
    void createOrder_insufficientStock_throwsBusinessException() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Empresa empresa = buildEmpresa(10L, user);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(user));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));

        Producto producto = Producto.builder()
                .id(100L)
                .nombre("Shampoo")
                .precio(new BigDecimal("25.00"))
                .stock(1)
                .empresa(empresa)
                .build();
        when(productoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(producto));

        CreateOrderDto dto = new CreateOrderDto(10L, null, null, null, null, null, null, null, null, null,
                List.of(new OrderItemDto(100L, null, 5)));

        assertThrows(BusinessException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void createOrder_emptyItems_savesOrderWithZeroTotals() {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Empresa empresa = buildEmpresa(10L, user);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(user));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        CreateOrderDto dto = new CreateOrderDto(10L, null, null, null, null, null, null, null, null, null,
                List.of());

        Long orderId = orderService.createOrder(dto);

        assertNotNull(orderId);
        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenRepository).save(captor.capture());
        Orden saved = captor.getValue();
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getTotal()));
    }

    @Test
    void createOrder_withCostoEnvio_calculatesTotalCorrectly() throws Exception {
        Usuario user = buildUser(1L, Role.CLIENTE);
        Empresa empresa = buildEmpresa(10L, user);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(user));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));

        Producto producto = Producto.builder()
                .id(100L)
                .nombre("Shampoo")
                .precio(new BigDecimal("100.00"))
                .stock(50)
                .empresa(empresa)
                .build();
        when(productoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(producto));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        lenient().when(ordenRepository.findByUsuarioClienteId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        CreateOrderDto dto = new CreateOrderDto(10L, null, null, null,
                new BigDecimal("15.00"), null, null, null, null, null,
                List.of(new OrderItemDto(100L, null, 1)));

        orderService.createOrder(dto);

        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenRepository).save(captor.capture());
        Orden saved = captor.getValue();
        assertEquals(new BigDecimal("100.00"), saved.getSubtotal());
        assertEquals(new BigDecimal("15.00"), saved.getCostoEnvio());
        assertEquals(new BigDecimal("115.00"), saved.getTotal());
    }
}
