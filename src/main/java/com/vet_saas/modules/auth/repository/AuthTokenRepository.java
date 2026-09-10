package com.vet_saas.modules.auth.repository;

import com.vet_saas.modules.auth.model.AuthToken;
import com.vet_saas.modules.auth.model.TokenType;
import com.vet_saas.modules.user.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHashAndTipo(String tokenHash, TokenType tipo);

    Optional<AuthToken> findByUsuarioAndTipo(Usuario usuario, TokenType tipo);

    void deleteByUsuarioAndTipo(Usuario usuario, TokenType tipo);
}
