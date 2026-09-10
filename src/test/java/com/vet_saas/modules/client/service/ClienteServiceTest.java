package com.vet_saas.modules.client.service;

import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.appointment.repository.CitaRepository;
import com.vet_saas.modules.client.dto.ClienteResponse;
import com.vet_saas.modules.client.dto.CreateClienteDto;
import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private EmpresaLookupService empresaLookupService;
    @Mock private PointsService pointsService;
    @Mock private AppProperties appProperties;
    @Mock private OrdenRepository ordenRepository;
    @Mock private CitaRepository citaRepository;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getBusiness()).thenReturn(new AppProperties.Business());
        clienteService = new ClienteService(clienteRepository, empresaLookupService, pointsService,
                appProperties, ordenRepository, citaRepository);
    }

    private Usuario buildUser(Long id) {
        return Usuario.builder()
                .id(id)
                .correo("client@test.com")
                .password("encoded")
                .rol(Role.CLIENTE)
                .estado(true)
                .build();
    }

    private PerfilCliente buildPerfil(Usuario user) {
        return PerfilCliente.builder()
                .id(1L)
                .usuario(user)
                .nombres("Juan")
                .apellidos("Perez")
                .telefono("999999999")
                .pais("Perú")
                .activo(true)
                .build();
    }

    @Test
    void createPerfil_success() {
        Usuario user = buildUser(1L);
        CreateClienteDto dto = new CreateClienteDto("Juan", "Perez", "999999999", "Av. Lima 123", "Lima", "Perú");

        when(clienteRepository.existsByUsuarioIdAndActivoTrue(1L)).thenReturn(false);
        when(clienteRepository.save(any(PerfilCliente.class))).thenAnswer(inv -> {
            PerfilCliente p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ClienteResponse response = clienteService.createPerfil(user, dto);

        assertNotNull(response);
        assertEquals("Juan", response.nombres());
        verify(pointsService).addPoints(eq(1L), eq("REGISTRO"), isNull(), anyString());
    }

    @Test
    void createPerfil_alreadyExists_throwsBusinessException() {
        Usuario user = buildUser(1L);
        when(clienteRepository.existsByUsuarioIdAndActivoTrue(1L)).thenReturn(true);

        CreateClienteDto dto = new CreateClienteDto("Juan", "Perez", "999999999", null, null, null);
        assertThrows(BusinessException.class, () -> clienteService.createPerfil(user, dto));
    }

    @Test
    void getMyPerfil_success() {
        Usuario user = buildUser(1L);
        PerfilCliente perfil = buildPerfil(user);
        when(clienteRepository.findByUsuarioIdAndActivoTrue(1L)).thenReturn(Optional.of(perfil));

        ClienteResponse response = clienteService.getMyPerfil(user);
        assertEquals("Juan", response.nombres());
    }

    @Test
    void getMyPerfil_notFound_throws() {
        Usuario user = buildUser(1L);
        when(clienteRepository.findByUsuarioIdAndActivoTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.getMyPerfil(user));
    }

    @Test
    void updateMyPerfil_updatesFields() {
        Usuario user = buildUser(1L);
        PerfilCliente perfil = buildPerfil(user);
        when(clienteRepository.findByUsuarioIdAndActivoTrue(1L)).thenReturn(Optional.of(perfil));
        when(clienteRepository.save(any(PerfilCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new com.vet_saas.modules.client.dto.UpdateClienteDto(
                "Carlos", null, null, null, null, null);
        ClienteResponse response = clienteService.updateMyPerfil(user, dto, null);

        assertEquals("Carlos", response.nombres());
    }

    @Test
    void deleteMiPerfil_success() {
        Usuario user = buildUser(1L);
        PerfilCliente perfil = buildPerfil(user);
        when(clienteRepository.findByUsuarioIdAndActivoTrue(1L)).thenReturn(Optional.of(perfil));
        when(clienteRepository.save(any(PerfilCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> clienteService.deleteMiPerfil(user));
        verify(clienteRepository).save(perfil);
        assertFalse(perfil.getActivo());
    }

    @Test
    void getClienteByIdAdmin_notFound_throws() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> clienteService.getClienteById(99L));
    }

    @Test
    void searchClientes_withQuery_delegatesToRepository() {
        when(clienteRepository.searchByNombre(eq("Juan"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = clienteService.searchClientes("Juan", PageRequest.of(0, 10));
        assertNotNull(result);
        verify(clienteRepository).searchByNombre("Juan", PageRequest.of(0, 10));
    }

    @Test
    void searchClientes_nullQuery_returnsAll() {
        when(clienteRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = clienteService.searchClientes(null, PageRequest.of(0, 10));
        assertNotNull(result);
    }
}
