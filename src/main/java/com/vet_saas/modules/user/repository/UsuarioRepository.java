package com.vet_saas.modules.user.repository;

import com.vet_saas.modules.user.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByAuth0Sub(String auth0Sub);
    Optional<Usuario> findByCodigoReferral(String codigoReferral);
}