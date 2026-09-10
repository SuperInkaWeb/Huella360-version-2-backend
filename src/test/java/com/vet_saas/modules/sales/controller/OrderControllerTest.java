package com.vet_saas.modules.sales.controller;

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
class OrderControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyOrders_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void getMyOrders_returns200_withClientRole() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void createOrder_returns400_withEmptyItems() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"empresaId": 1, "items": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void createOrder_returns400_withMissingItems() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"empresaId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returns401_withoutAuth() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"empresaId": 1, "items": [{"productoId": 1, "cantidad": 1}]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void getMyOrders_withFilters_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .param("page", "0")
                        .param("size", "5")
                        .param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
