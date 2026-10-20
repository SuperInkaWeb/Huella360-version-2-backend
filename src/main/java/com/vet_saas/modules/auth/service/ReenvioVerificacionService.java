package com.vet_saas.modules.auth.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.auth.model.TokenType;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * H360-SEC-002: reenvia el enlace de verificacion del correo.
 * Usuarios de Auth0 -> lo envia Auth0 (Management API). Usuarios del registro propio -> enlace propio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReenvioVerificacionService {

    static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofMinutes(1);

    private final Auth0ManagementClient auth0ManagementClient;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;

    private final Map<Long, Instant> ultimoEnvio = new ConcurrentHashMap<>();

    public void reenviar(Usuario usuario) {
        if (usuario.isEmailVerificado()) {
            throw new BusinessException("Tu correo ya está verificado.");
        }
        Instant ahora = Instant.now();
        Instant anterior = ultimoEnvio.get(usuario.getId());
        if (anterior != null && ahora.isBefore(anterior.plus(ESPERA_ENTRE_ENVIOS))) {
            throw new BusinessException("Ya te enviamos un enlace hace poco. Espera un minuto antes de pedir otro.");
        }

        if (StringUtils.hasText(usuario.getAuth0Sub())) {
            if (!auth0ManagementClient.estaConfigurado()) {
                log.warn("Reenvio de verificacion pedido, pero falta configurar AUTH0_MGMT_CLIENT_ID/SECRET");
                throw new BusinessException("Por ahora no podemos reenviar el enlace. Inténtalo más tarde o contacta a soporte.");
            }
            try {
                auth0ManagementClient.reenviarCorreoVerificacion(usuario.getAuth0Sub());
            } catch (RuntimeException e) {
                log.error("Auth0 rechazo el reenvio de verificacion. Usuario ID: {}: {}", usuario.getId(), e.getMessage());
                throw new BusinessException("No pudimos reenviar el enlace. Inténtalo de nuevo en unos minutos.");
            }
        } else {
            String token = authTokenService.createToken(usuario, TokenType.EMAIL_VERIFICATION, 24);
            emailService.sendVerificationEmail(usuario, token);
        }
        ultimoEnvio.put(usuario.getId(), ahora);
    }
}
