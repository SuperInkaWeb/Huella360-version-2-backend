package com.vet_saas.modules.admin.service;

import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceListadosTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;

    @InjectMocks
    private AdminService adminService;

    // Pagina 2 para comprobar que se pasa tal cual al repositorio.
    private final Pageable pagina = PageRequest.of(2, 20);
    private final Pageable primera = PageRequest.of(0, 20);

    @Test
    void normalizarBusqueda_nuloOEspaciosEsSinFiltro() {
        assertEquals("", AdminService.normalizarBusqueda(null));
        assertEquals("", AdminService.normalizarBusqueda("   "));
        assertEquals("ruc 2099", AdminService.normalizarBusqueda("  ruc 2099 "));
    }

    @Test
    void getAllUsers_buscaEnServidorConLaPaginaPedida() {
        Usuario u = Usuario.builder().id(1L).correo("cliente@test.com").build();
        when(usuarioRepository.buscarParaAdmin("cliente", pagina)).thenReturn(new PageImpl<>(List.of(u), pagina, 41));

        var res = adminService.getAllUsers(" cliente ", pagina);

        assertEquals(41, res.getTotalElements());
        assertEquals("cliente@test.com", res.getContent().get(0).getCorreo());
    }

    @Test
    void getAllCompanies_sinEstadoNoFiltraPorEstado() {
        when(empresaRepository.buscarParaAdmin("", pagina)).thenReturn(Page.empty(pagina));

        adminService.getAllCompanies(null, null, pagina);

        verify(empresaRepository, never()).buscarParaAdminPorEstado(any(), any(), any());
    }

    @Test
    void getAllCompanies_conEstadoFiltraPorEstado() {
        Empresa e = Empresa.builder().id(7L).nombreComercial("Pelos SAC").estadoValidacion(VerificationStatus.PENDIENTE).build();
        when(empresaRepository.buscarParaAdminPorEstado("", VerificationStatus.PENDIENTE, primera))
                .thenReturn(new PageImpl<>(List.of(e), primera, 1));

        var res = adminService.getAllCompanies("", VerificationStatus.PENDIENTE, primera);

        assertEquals("Pelos SAC", res.getContent().get(0).getNombreComercial());
        verify(empresaRepository, never()).buscarParaAdmin(any(), any());
    }

    @Test
    void getAllVeterinarios_conEstadoFiltraPorEstado() {
        Veterinario v = Veterinario.builder().id(3L).nombres("Jose").estadoValidacion(VerificationStatus.VERIFICADO).build();
        when(veterinarioRepository.buscarParaAdminPorEstado("perez", VerificationStatus.VERIFICADO, primera))
                .thenReturn(new PageImpl<>(List.of(v), primera, 1));

        var res = adminService.getAllVeterinarios("perez", VerificationStatus.VERIFICADO, primera);

        assertEquals(1, res.getTotalElements());
        verify(veterinarioRepository, never()).buscarParaAdmin(any(), any());
    }
}
