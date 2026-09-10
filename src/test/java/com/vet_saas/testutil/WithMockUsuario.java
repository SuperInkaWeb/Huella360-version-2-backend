package com.vet_saas.testutil;

import com.vet_saas.modules.user.model.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUsuarioSecurityContextFactory.class)
public @interface WithMockUsuario {
    String correo() default "test@test.com";
    Role role() default Role.CLIENTE;
    long id() default 1L;
    boolean estado() default true;
}
