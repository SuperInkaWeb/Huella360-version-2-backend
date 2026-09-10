package com.vet_saas.modules.security;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthorizationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    private void authenticateAs(Role role) {
        Usuario user = Usuario.builder()
                .correo("auth-" + role.name().toLowerCase() + "@test.com")
                .password("encoded")
                .rol(role)
                .estado(true)
                .build();
        usuarioRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ====================================================================
    // 401: Todos los endpoints protegidos rechazan requests sin token
    // ====================================================================

    @Test
    void reclamos_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(post("/api/v1/reclamos")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void orders_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void medicalRecords_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/medical-records/pet/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staff_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/companies/staff"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void company_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/companies/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentSync_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/payments/sync")
                        .param("payment_id", "123")
                        .param("external_reference", "ORD-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentCheckout_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(post("/api/v1/payments/checkout/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adoptions_me_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/adoptions/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appointments_me_returns401_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/appointments/me"))
                .andExpect(status().isUnauthorized());
    }

    // ====================================================================
    // 200: Endpoints públicos accesibles sin autenticación
    // ====================================================================

    @Test
    void publicCategories_returns200_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void userExists_returns200_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/users/exists/test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void publicServices_returns200_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk());
    }

    @Test
    void subscriptionPlans_returns200_withoutAuth() throws Exception {
        clearAuth();
        mockMvc.perform(get("/api/v1/subscriptions/plans"))
                .andExpect(status().isOk());
    }

    // ====================================================================
    // 200: Endpoints autenticados con rol correcto
    // ====================================================================

    @Test
    void medicalRecords_returns200_withAuthenticatedUser() throws Exception {
        authenticateAs(Role.CLIENTE);
        mockMvc.perform(get("/api/v1/medical-records/pet/1"))
                .andExpect(status().isOk());
    }

    @Test
    void adoptions_me_returns200_withAuthenticatedUser() throws Exception {
        authenticateAs(Role.CLIENTE);
        mockMvc.perform(get("/api/v1/adoptions/me"))
                .andExpect(status().isOk());
    }

    @Test
    void appointments_me_returns200_withClientRole() throws Exception {
        authenticateAs(Role.CLIENTE);
        mockMvc.perform(get("/api/v1/appointments/me"))
                .andExpect(status().isOk());
    }

    @Test
    void vet_me_returns403_withClientRole() throws Exception {
        authenticateAs(Role.CLIENTE);
        mockMvc.perform(get("/api/v1/veterinarians/me"))
                .andExpect(status().isForbidden());
    }
}
