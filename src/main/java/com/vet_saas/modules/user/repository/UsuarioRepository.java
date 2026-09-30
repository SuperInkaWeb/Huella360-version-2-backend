package com.vet_saas.modules.user.repository;

import com.vet_saas.modules.user.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // La columna `correo` es citext, pero Hibernate liga el parametro como VARCHAR,
    // lo que hace que Postgres use comparacion case-sensitive en vez del operador
    // case-insensitive de citext. Forzamos LOWER() en ambos lados para que el
    // login no falle cuando el correo se escribe con distinta capitalizacion.
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.correo) = LOWER(:correo)")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);
    Optional<Usuario> findByAuth0Sub(String auth0Sub);
    Optional<Usuario> findByCodigoReferral(String codigoReferral);

    // Busqueda del panel admin. q vacio = todos. Se busca en servidor para no depender
    // de la pagina cargada en el navegador (antes solo se veian los primeros 50).
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Usuario> buscarParaAdmin(@Param("q") String q, Pageable pageable);
}