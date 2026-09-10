package com.vet_saas.modules.referral.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.referral.dto.ReferralCountResponse;
import com.vet_saas.modules.referral.model.Referido;
import com.vet_saas.modules.referral.repository.ReferidoRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferidoRepository referidoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final long REFERIDOS_PARA_DESBLOQUEO = 10;
    private static final int CODIGO_LONGITUD = 8;
    private static final String CODIGO_PREFIJO = "H360-";
    private static final String CODIGO_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Obtiene (o genera) el código de referido único del usuario.
     * El código se persiste en la tabla usuarios y no cambia.
     */
    @Transactional
    public String getOrGenerateReferralCode(Usuario usuario) {
        if (usuario.getCodigoReferral() != null) {
            return usuario.getCodigoReferral();
        }

        String nuevoCodigo;
        do {
            nuevoCodigo = CODIGO_PREFIJO + generarCodigoAleatorio(CODIGO_LONGITUD);
        } while (usuarioRepository.findByCodigoReferral(nuevoCodigo).isPresent());

        usuario.setCodigoReferral(nuevoCodigo);
        usuarioRepository.save(usuario);
        log.info("Código de referido generado para usuario {}: {}", usuario.getId(), nuevoCodigo);
        return nuevoCodigo;
    }

    /**
     * Aplica un código de referido. Crea un registro en la tabla referidos
     * vinculando al usuario que refiere con el nuevo usuario.
     */
    @Transactional
    public void applyReferralCode(Usuario nuevoUsuario, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código de referido es obligatorio");
        }

        Usuario referrer = usuarioRepository.findByCodigoReferral(codigo)
                .orElseThrow(() -> new BusinessException("Código de referido inválido"));

        if (referrer.getId().equals(nuevoUsuario.getId())) {
            throw new BusinessException("No puedes usar tu propio código de referido");
        }

        boolean alreadyReferred = referidoRepository.existsByUsuarioRefiridoIdAndNotSelfReference(
                nuevoUsuario.getId(), nuevoUsuario.getId());
        if (alreadyReferred) {
            throw new BusinessException("Ya has sido referido por otro usuario");
        }

        boolean alreadyApplied = referidoRepository
                .existsByUsuarioRefiridoIdAndUsuarioQueRefirioId(nuevoUsuario.getId(), referrer.getId());
        if (alreadyApplied) {
            throw new BusinessException("Ya has aplicado este código de referido");
        }

        Referido newReferido = Referido.builder()
                .usuarioQueRefirio(referrer)
                .usuarioRefirido(nuevoUsuario)
                .codigoReferido(codigo)
                .build();

        try {
            referidoRepository.save(newReferido);
            log.info("Usuario {} referido por usuario {}", nuevoUsuario.getId(), referrer.getId());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Este código de referido ya ha sido utilizado");
        }
    }

    /**
     * Cuenta cuántos usuarios ha referido uno dado.
     */
    @Transactional(readOnly = true)
    public ReferralCountResponse getReferralCount(Long usuarioId) {
        long count = referidoRepository.countByUsuarioQueRefirioId(usuarioId);
        boolean desbloqueado = count >= REFERIDOS_PARA_DESBLOQUEO;

        return ReferralCountResponse.builder()
                .totalReferidos(count)
                .desbloqueo2daMascota(desbloqueado)
                .referidosNecesarios(REFERIDOS_PARA_DESBLOQUEO)
                .referidosRestantes(Math.max(0, REFERIDOS_PARA_DESBLOQUEO - count))
                .build();
    }

    @Transactional(readOnly = true)
    public ReferralCountResponse getReferralCount(Usuario usuario) {
        return getReferralCount(usuario.getId());
    }

    private String generarCodigoAleatorio(int longitud) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CODIGO_CHARS.charAt(random.nextInt(CODIGO_CHARS.length())));
        }
        return sb.toString();
    }
}
