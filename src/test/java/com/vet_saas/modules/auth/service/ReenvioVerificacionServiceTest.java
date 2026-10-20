package com.vet_saas.modules.auth.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.auth.model.TokenType;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReenvioVerificacionServiceTest {

    @Mock private Auth0ManagementClient auth0ManagementClient;
    @Mock private AuthTokenService authTokenService;
    @Mock private EmailService emailService;
    @InjectMocks private ReenvioVerificacionService service;

    @Test
    void usuarioDeAuth0_loReenviaAuth0() {
        Usuario u = usuario(1L, "auth0|abc");
        when(auth0ManagementClient.estaConfigurado()).thenReturn(true);

        service.reenviar(u);

        verify(auth0ManagementClient).reenviarCorreoVerificacion("auth0|abc");
        verifyNoInteractions(emailService);
    }

    @Test
    void usuarioDelRegistroPropio_usaElEnlacePropio() {
        Usuario u = usuario(2L, null);
        when(authTokenService.createToken(u, TokenType.EMAIL_VERIFICATION, 24)).thenReturn("tok");

        service.reenviar(u);

        verify(emailService).sendVerificationEmail(u, "tok");
        verifyNoInteractions(auth0ManagementClient);
    }

    @Test
    void correoYaVerificado_noReenvia() {
        Usuario u = usuario(3L, "auth0|abc");
        u.setEmailVerificado(true);

        assertThatThrownBy(() -> service.reenviar(u)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(auth0ManagementClient, emailService);
    }

    @Test
    void segundoPedidoEnMenosDeUnMinuto_seRechaza() {
        Usuario u = usuario(4L, "auth0|abc");
        when(auth0ManagementClient.estaConfigurado()).thenReturn(true);
        service.reenviar(u);

        assertThatThrownBy(() -> service.reenviar(u))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Espera un minuto");
        verify(auth0ManagementClient, times(1)).reenviarCorreoVerificacion(anyString());
    }

    @Test
    void sinCredencialesDeManagement_avisaSinFallar() {
        Usuario u = usuario(5L, "auth0|abc");
        when(auth0ManagementClient.estaConfigurado()).thenReturn(false);

        assertThatThrownBy(() -> service.reenviar(u)).isInstanceOf(BusinessException.class);
        verify(auth0ManagementClient, never()).reenviarCorreoVerificacion(anyString());
    }

    @Test
    void siAuth0Falla_sePuedeReintentarDeInmediato() {
        Usuario u = usuario(6L, "auth0|abc");
        when(auth0ManagementClient.estaConfigurado()).thenReturn(true);
        doThrow(new RuntimeException("503")).doNothing().when(auth0ManagementClient).reenviarCorreoVerificacion("auth0|abc");

        assertThatThrownBy(() -> service.reenviar(u)).isInstanceOf(BusinessException.class);
        service.reenviar(u);

        verify(auth0ManagementClient, times(2)).reenviarCorreoVerificacion("auth0|abc");
    }

    private static Usuario usuario(Long id, String auth0Sub) {
        return Usuario.builder().id(id).correo("u" + id + "@test.com").auth0Sub(auth0Sub)
                .estado(true).emailVerificado(false).build();
    }
}
