package com.vet_saas.modules.pet.controller;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.testutil.WithMockUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PetControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyPets_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/pets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void getMyPets_returns200_withClientRole() throws Exception {
        mockMvc.perform(get("/api/v1/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUsuario(role = Role.EMPRESA)
    void getMyPets_returns200_withEmpresaRole() throws Exception {
        mockMvc.perform(get("/api/v1/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void getPetById_returns404_whenNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/pets/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPet_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(multipart("/api/v1/pets")
                        .file("data", """
                                {"nombre":"Max","especie":"Perro"}
                                """.getBytes())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void deletePet_returns404_whenNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/pets/99999"))
                .andExpect(status().isNotFound());
    }
}
