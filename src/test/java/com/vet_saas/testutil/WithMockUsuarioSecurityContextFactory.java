package com.vet_saas.testutil;

import com.vet_saas.modules.user.model.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockUsuarioSecurityContextFactory implements WithSecurityContextFactory<WithMockUsuario> {

    @Override
    public SecurityContext createSecurityContext(WithMockUsuario annotation) {
        Usuario user = Usuario.builder()
                .id(annotation.id())
                .correo(annotation.correo())
                .password("encoded")
                .rol(annotation.role())
                .estado(annotation.estado())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
