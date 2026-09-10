package com.vet_saas.security.jwt;

import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;

/**
 * Convierte un JWT decodificado (Auth0 o legacy) en un Authentication de Spring Security.
 * <p>
 * Para tokens Auth0 (RS256):
 * - Busca el usuario por email (claim "sub" o "email" o "https://vet-saas.com/email")
 * - Si no existe, lo crea automáticamente (primer login de Auth0)
 * - Extrae el rol del claim "https://vet-saas.com/roles" o "role"
 * <p>
 * Para tokens legacy (HS256):
 * - Busca el usuario por ID (claim "sub")
 * - Extrae el rol del claim "role"
 */
@Slf4j
@RequiredArgsConstructor
public class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    // Claims personalizados de Auth0
    private static final String AUTH0_EMAIL_CLAIM = "https://vet-saas.com/email";
    private static final String AUTH0_EMAIL_CLAIM_ALT = "https://huella360.com/email";
    private static final String AUTH0_ROLES_CLAIM = "https://vet-saas.com/roles";
    private static final String AUTH0_ROLES_CLAIM_ALT = "https://huella360.com/role";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            String algorithm = (String) jwt.getHeaders().get("alg");
            log.debug("Convirtiendo JWT. Algoritmo: {}, Subject: {}", algorithm, jwt.getSubject());

            if ("HS256".equals(algorithm) || "HS384".equals(algorithm) || "HS512".equals(algorithm)) {
                return convertLegacyToken(jwt);
            } else {
                return convertAuth0Token(jwt);
            }
        } catch (Exception e) {
            log.error("Error convirtiendo JWT a Authentication: {}", e.getMessage(), e);
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Error procesando token JWT: " + e.getMessage(), e);
        }
    }

    // ─── Auth0 Token Conversion ──────────────────────────────────────────────

    private AbstractAuthenticationToken convertAuth0Token(Jwt jwt) {
        // Try to find by email first
        String email = extractEmailFromAuth0Claims(jwt);
        Usuario usuario = null;

        if (email != null && !email.isBlank()) {
            log.debug("Buscando usuario por email: {}", email);
            usuario = usuarioRepository.findByCorreo(email).orElse(null);
        }

        // If not found by email, try by auth0_sub
        if (usuario == null) {
            String subject = jwt.getSubject();
            if (subject != null && subject.startsWith("auth0|")) {
                log.debug("Buscando usuario por auth0_sub: {}", subject);
                usuario = usuarioRepository.findByAuth0Sub(subject).orElse(null);
            }
        }

        // Auto-crear usuario en el primer login de Auth0
        if (usuario == null && email != null && !email.isBlank()) {
            log.info("Usuario no encontrado. Creando usuario desde Auth0. Email: {}, Subject: {}", email, jwt.getSubject());
            usuario = Usuario.builder()
                    .correo(email)
                    .password("[AUTH0]")  // Placeholder - Auth0 maneja la contraseña
                    .auth0Sub(jwt.getSubject())
                    .rol(null)  // Sin rol por defecto - el usuario debe seleccionarlo
                    .emailVerificado(true)
                    .build();
            usuario = usuarioRepository.save(usuario);
            log.info("Usuario creado exitosamente desde Auth0. ID: {}", usuario.getId());
        }

        if (usuario == null) {
            log.warn("Usuario no encontrado y no se pudo crear. Email: {}, Subject: {}", email, jwt.getSubject());
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "No se pudo autenticar. Asegurate de completar el registro en la plataforma.");
        }

        // Usar rol de la BD como fuente authoritative, fallback al JWT
        Role rol = usuario.getRol();
        if (rol == null) {
            rol = extractRoleFromAuth0Claims(jwt);
        }

        // Si el usuario no tiene rol, permitir autenticación básica para que pueda seleccionar rol
        List<SimpleGrantedAuthority> authorities;
        if (rol == null) {
            log.debug("Usuario sin rol definido: {}. Permitiendo autenticación para selección de rol.", usuario.getCorreo());
            authorities = Collections.emptyList();
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        }

        log.debug("Usuario autenticado: {}, Rol: {}", usuario.getCorreo(), rol);

        return new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                authorities
        );
    }

    private String extractEmailFromAuth0Claims(Jwt jwt) {
        // Intentar claim personalizado primero (vet-saas.com)
        String email = jwt.getClaimAsString(AUTH0_EMAIL_CLAIM);
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Intentar claim alternativo (huella360.com)
        email = jwt.getClaimAsString(AUTH0_EMAIL_CLAIM_ALT);
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Intentar claim "email" estandar
        email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Intentar claim "nickname" de Auth0 (a veces viene)
        email = jwt.getClaimAsString("nickname");
        if (email != null && !email.isBlank() && email.contains("@")) {
            return email;
        }

        // NO usar "sub" como fallback - auth0|xxx no es un email
        log.warn("No se encontro email en claims del JWT. Claims disponibles: {}", jwt.getClaims());
        return null;
    }

    private Role extractRoleFromAuth0Claims(Jwt jwt) {
        // Intentar claim personalizado de Auth0 (vet-saas.com)
        List<String> roles = jwt.getClaimAsStringList(AUTH0_ROLES_CLAIM);
        if (roles != null && !roles.isEmpty()) {
            try {
                return Role.valueOf(roles.get(0).toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Rol no valido en claim Auth0: {}", roles.get(0));
            }
        }

        // Intentar claim alternativo (huella360.com)
        String roleAlt = jwt.getClaimAsString(AUTH0_ROLES_CLAIM_ALT);
        if (roleAlt != null && !roleAlt.isBlank()) {
            try {
                return Role.valueOf(roleAlt.toUpperCase().replace("ROLE_", ""));
            } catch (IllegalArgumentException e) {
                log.warn("Rol no valido en claim alternativo: {}", roleAlt);
            }
        }

        // Intentar claim "role" individual
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            try {
                return Role.valueOf(role.toUpperCase().replace("ROLE_", ""));
            } catch (IllegalArgumentException e) {
                log.warn("Rol no valido en claim 'role': {}", role);
            }
        }

        // Default: sin rol - el usuario debe seleccionarlo en el frontend
        log.info("No se encontro rol en JWT, usuario debe seleccionar rol en el frontend");
        return null;
    }

    // ─── Legacy Token Conversion ─────────────────────────────────────────────

    private AbstractAuthenticationToken convertLegacyToken(Jwt jwt) {
        // El "sub" del token legacy es el ID numerico del usuario
        String subject = jwt.getSubject();

        Long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Subject del token legacy no es un ID numerico: {}", subject);
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Subject del token legacy no es un ID numerico: " + subject);
        }

        log.debug("Buscando usuario por ID: {}", userId);
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Usuario no encontrado para ID: " + userId));

        // Extraer rol del claim "role"
        String roleStr = jwt.getClaimAsString("role");
        Role rol;
        try {
            rol = Role.valueOf(roleStr.toUpperCase().replace("ROLE_", ""));
        } catch (Exception e) {
            rol = usuario.getRol(); // Fallback al rol de la BD
        }

        log.debug("Usuario encontrado: {}, Rol: {}", usuario.getCorreo(), rol);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + rol.name())
        );

        return new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                authorities
        );
    }
}
