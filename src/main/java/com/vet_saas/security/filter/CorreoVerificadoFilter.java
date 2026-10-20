package com.vet_saas.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * H360-SEC-002: una cuenta con el correo sin verificar solo puede usar lo publico, ver su
 * estado (/users/me) y las rutas de /auth (reenviar el correo, cerrar sesion). Todo lo demas
 * responde 403 con error=EMAIL_NOT_VERIFIED para que el frontend muestre la pantalla de verificacion.
 * <p>
 * Se activa con app.security.exigir-correo-verificado. Debe encenderse solo cuando la Action de
 * Auth0 ya envie email_verified: antes de eso hay cuentas marcadas sin verificar que no tienen
 * forma de corregirse (p. ej. clientes invitados creados al agendar una cita).
 */
@Slf4j
public class CorreoVerificadoFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE = "EMAIL_NOT_VERIFIED";
    public static final String MENSAJE =
            "Verifica tu correo para usar tu cuenta. Revisa tu bandeja de entrada o pide un nuevo enlace.";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    // Mismas rutas abiertas que SecurityConfig, mas las que necesita la pantalla de verificacion.
    private static final List<String> PERMITIDAS = List.of(
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/api/v1/users/exists/**",
            "/api/v1/companies/public/**",
            "/api/v1/payments/webhook/**",
            "/payment/**",
            "/error",
            "/actuator/health",
            "/actuator/info");

    private static final List<String> PERMITIDAS_GET = List.of(
            "/api/v1/users/me",
            "/api/v1/adoptions",
            "/api/v1/adoptions/**",
            "/api/v1/categories",
            "/api/v1/categories/**",
            "/api/v1/services",
            "/api/v1/services/**",
            "/api/v1/appointments/available-slots",
            "/api/v1/appointments/slots",
            "/api/v1/subscriptions/plans");

    private static final List<String> PERMITIDAS_POST = List.of(
            "/api/v1/newsletter/**",
            "/api/v1/contacto",
            "/api/v1/reclamos");

    private final boolean exigirCorreoVerificado;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public CorreoVerificadoFilter(boolean exigirCorreoVerificado) {
        this.exigirCorreoVerificado = exigirCorreoVerificado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Usuario usuario = usuarioAutenticado();
        if (exigirCorreoVerificado && usuario != null && !usuario.isEmailVerificado()
                && !esPermitida(request.getMethod(), request.getRequestURI())) {
            log.debug("Acceso bloqueado por correo sin verificar. Usuario ID: {}, ruta: {}",
                    usuario.getId(), request.getRequestURI());
            responderBloqueo(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean esPermitida(String metodo, String ruta) {
        if (coincide(PERMITIDAS, ruta)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(metodo)) {
            return coincide(PERMITIDAS_GET, ruta);
        }
        if ("POST".equalsIgnoreCase(metodo)) {
            return coincide(PERMITIDAS_POST, ruta);
        }
        return "OPTIONS".equalsIgnoreCase(metodo);
    }

    private static boolean coincide(List<String> patrones, String ruta) {
        return patrones.stream().anyMatch(p -> MATCHER.match(p, ruta));
    }

    private static Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Usuario u ? u : null;
    }

    private void responderBloqueo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "success", false,
                "status", 403,
                "error", ERROR_CODE,
                "message", MENSAJE,
                "path", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
