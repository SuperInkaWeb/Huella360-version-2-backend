package com.vet_saas.modules.auth.service;

import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.UnauthorizedException;
import com.vet_saas.modules.auth.dto.AuthResponse;
import com.vet_saas.modules.auth.model.RefreshToken;
import com.vet_saas.modules.auth.repository.RefreshTokenRepository;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse createRefreshToken(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        // Revocar tokens previos del usuario al crear uno nuevo (login/register)
        refreshTokenRepository.revokeAllByUserId(userId);

        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshExpiration()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateToken(usuario);

        return new AuthResponse(accessToken, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        String inputHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(inputHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido o inexistente"));

        // Detección de reuso: si el token ya fue revocado, alguien lo robó
        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Reuse detected for user {}", storedToken.getUsuario().getId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUsuario().getId());
            throw new UnauthorizedException("Sesión comprometida. Por favor inicie sesión nuevamente.");
        }

        // Validar expiración
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Sesión expirada. Inicie sesión nuevamente.");
        }

        // Revocar el token actual (rotación)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Emitir nuevo par de tokens
        return createNewTokenPair(storedToken.getUsuario());
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String inputHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(inputHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAllByUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked for user {}", userId);
    }

    private AuthResponse createNewTokenPair(Usuario usuario) {
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshExpiration()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateToken(usuario);

        return new AuthResponse(accessToken, rawRefreshToken);
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

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}