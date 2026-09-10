package com.vet_saas.modules.company.controller;

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
class CompanyControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyCompany_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/companies/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUsuario(role = Role.EMPRESA)
    void getMyCompany_returns404_whenNoProfile() throws Exception {
        mockMvc.perform(get("/api/v1/companies/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPublicCompanies_returns200_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/companies/public")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void createProfile_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(multipart("/api/v1/companies")
                        .file("data", """
                                {"nombreComercial":"Pet Shop","ruc":"12345678901"}
                                """.getBytes())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }
}
