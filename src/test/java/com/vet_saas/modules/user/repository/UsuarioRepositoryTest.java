package com.vet_saas.modules.user.repository;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void findByCorreo_isCaseInsensitive_regressionForH360Auth001() {
        // H360-AUTH-001: la columna `correo` es citext (case-insensitive por diseno),
        // pero Hibernate liga el parametro de esta query como VARCHAR, asi que sin el
        // LOWER() explicito Postgres comparaba en modo case-sensitive y un usuario
        // registrado con el correo autocapitalizado por su teclado no podia loguearse.
        Usuario usuario = Usuario.builder()
                .correo("Cliente@Test.com")
                .password("encoded-password")
                .rol(Role.CLIENTE)
                .estado(true)
                .emailVerificado(true)
                .build();
        usuarioRepository.save(usuario);

        Optional<Usuario> porMinusculas = usuarioRepository.findByCorreo("cliente@test.com");
        Optional<Usuario> porMayusculas = usuarioRepository.findByCorreo("CLIENTE@TEST.COM");
        Optional<Usuario> porCapitalizacionOriginal = usuarioRepository.findByCorreo("Cliente@Test.com");

        assertTrue(porMinusculas.isPresent(), "Deberia encontrar el usuario buscando el correo en minusculas");
        assertTrue(porMayusculas.isPresent(), "Deberia encontrar el usuario buscando el correo en mayusculas");
        assertTrue(porCapitalizacionOriginal.isPresent(), "Deberia encontrar el usuario con la capitalizacion original");

        assertEquals(usuario.getId(), porMinusculas.get().getId());
        assertEquals(usuario.getId(), porMayusculas.get().getId());
        assertEquals(usuario.getId(), porCapitalizacionOriginal.get().getId());
    }

    @Test
    void findByCorreo_returnsEmpty_whenCorreoDoesNotExist() {
        Optional<Usuario> resultado = usuarioRepository.findByCorreo("nadie@test.com");

        assertTrue(resultado.isEmpty());
    }
}
