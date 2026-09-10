package com.vet_saas.modules.auth.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.auth.model.AuthToken;
import com.vet_saas.modules.auth.model.TokenType;
import com.vet_saas.modules.auth.repository.AuthTokenRepository;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;

    @Transactional
    public String createToken(Usuario usuario, TokenType tipo, int expirationHours) {
        authTokenRepository.deleteByUsuarioAndTipo(usuario, tipo);

        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = hashToken(tokenValue);

        AuthToken token = AuthToken.builder()
                .usuario(usuario)
                .tokenHash(tokenHash)
                .tipo(tipo)
                .fechaExpiracion(LocalDateTime.now().plusHours(expirationHours))
                .build();

        authTokenRepository.save(token);
        return tokenValue;
    }

    public AuthToken validateToken(String tokenValue, TokenType tipo) {
        String tokenHash = hashToken(tokenValue);
        AuthToken token = authTokenRepository.findByTokenHashAndTipo(tokenHash, tipo)
                .orElseThrow(() -> new BusinessException("Token inválido o no encontrado"));

        if (token.isExpired()) {
            authTokenRepository.delete(token);
            throw new BusinessException("El token ha expirado");
        }

        return token;
    }

    @Transactional
    public void deleteToken(AuthToken token) {
        authTokenRepository.delete(token);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este entorno", e);
        }
    }
}
