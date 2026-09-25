package com.vet_saas.modules.points.controller;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.client.model.PerfilCliente;
import com.vet_saas.modules.client.repository.ClienteRepository;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.points.service.PointsConfigService;
import com.vet_saas.modules.points.service.PointsService;
import com.vet_saas.modules.points.service.RewardService;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * H360: los puntos y canjes se guardan por id de PerfilCliente, pero los endpoints del cliente
 * pasaban usuario.getId(). Confirmado en QA el 2026-09-25: qa-render-cliente03 (usuarioId 11,
 * perfilId 3) veia 0 puntos pese a tener bonos de registro y primera mascota. Cuando el id de
 * usuario de alguien coincide con el id de perfil de otro cliente, veia y gastaba sus puntos.
 */
@ExtendWith(MockitoExtension.class)
class PointsIdPerfilControllerTest {

    @Mock private PointsService pointsService;
    @Mock private PointsConfigService configService;
    @Mock private RewardService rewardService;
    @Mock private UsuarioService usuarioService;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private ClienteRepository clienteRepository;

    private PointsController pointsController;
    private RewardController rewardController;

    private final Principal principal = () -> "cliente03@test.com";
    private final Usuario usuario = Usuario.builder().id(11L).correo("cliente03@test.com").rol(Role.CLIENTE).build();

    @BeforeEach
    void setUp() {
        pointsController = new PointsController(pointsService, configService, usuarioService, clienteRepository);
        rewardController = new RewardController(rewardService, usuarioService, empresaRepository, clienteRepository);
        when(usuarioService.findByCorreo("cliente03@test.com")).thenReturn(usuario);
    }

    private void conPerfil(Long perfilId) {
        when(clienteRepository.findByUsuarioId(11L))
                .thenReturn(Optional.of(PerfilCliente.builder().id(perfilId).usuario(usuario).build()));
    }

    @Test
    void dashboardDePuntos_usaElIdDelPerfilNoElDelUsuario() {
        conPerfil(3L);

        pointsController.getMyPointsDashboard(principal);

        verify(pointsService).getClientDashboard(3L);
        verify(pointsService, never()).getClientDashboard(11L);
    }

    @Test
    void canjearRecompensa_descuentaDelPerfilDelCliente() {
        conPerfil(3L);

        rewardController.redeemReward(7L, principal);

        verify(rewardService).redeemReward(3L, 7L);
        verify(rewardService, never()).redeemReward(eq(11L), anyLong());
    }

    @Test
    void misRecompensasYCheckout_usanElIdDelPerfil() {
        conPerfil(3L);

        rewardController.getMyRedeemedRewards(Pageable.unpaged(), principal);
        rewardController.getAvailableRewardsForCheckout(5L, principal);

        verify(rewardService).getMyRedeemedRewards(eq(3L), any());
        verify(rewardService).getAvailableRewardsForCheckout(3L, 5L);
    }

    @Test
    void usuarioSinPerfilDeCliente_noCaeEnElPerfilDeOtro() {
        when(clienteRepository.findByUsuarioId(11L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pointsController.getMyPointsDashboard(principal));
        assertThrows(ResourceNotFoundException.class, () -> rewardController.redeemReward(7L, principal));
        verifyNoInteractions(pointsService, rewardService);
    }
}
