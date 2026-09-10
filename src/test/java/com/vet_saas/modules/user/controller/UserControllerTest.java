package com.vet_saas.modules.user.controller;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario testUser;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        testUser = Usuario.builder()
                .correo("test@test.com")
                .password("encoded-password")
                .rol(Role.CLIENTE)
                .estado(true)
                .emailVerificado(true)
                .build();
        testUser = usuarioRepository.save(testUser);
    }

    private void authenticateAs(Usuario usuario) {
        var auth = new UsernamePasswordAuthenticationToken(
                usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMe_returnsUserInfo() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.correo").value("test@test.com"))
                .andExpect(jsonPath("$.data.rol").value("CLIENTE"));
    }

    @Test
    void getMe_returns403_whenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyRole_changesFromClientToEmpresa() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"EMPRESA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rol").value("EMPRESA"));

        Usuario updated = usuarioRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(Role.EMPRESA, updated.getRol());
    }

    @Test
    void updateMyRole_rejectsAdmin() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyRole_rejects_whenAlreadyNonClient() throws Exception {
        testUser.setRol(Role.EMPRESA);
        usuarioRepository.save(testUser);
        authenticateAs(testUser);

        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"VETERINARIO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyRole_rejectsInvalidRole() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"INVALID_ROLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyRole_rejectsMissingRole() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyRole_acceptsRepartidor() throws Exception {
        authenticateAs(testUser);
        mockMvc.perform(patch("/api/v1/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"REPARTIDOR\"}"))
                .andExpect(status().isOk());
    }
}
