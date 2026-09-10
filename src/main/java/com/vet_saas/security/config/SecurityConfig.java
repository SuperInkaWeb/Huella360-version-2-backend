package com.vet_saas.security.config;

import com.vet_saas.config.AppProperties;
import com.vet_saas.security.jwt.Auth0JwtAuthenticationConverter;
import com.vet_saas.security.jwt.Auth0JwtDecoder;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtEntryPoint;
    private final AppProperties appProperties;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.auth0.issuer}")
    private String auth0IssuerUri;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtEntryPoint))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/sync").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/users/exists/**").permitAll()
                        .requestMatchers("/api/v1/companies/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/adoptions/public/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/adoptions/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/adoptions/applications/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/adoptions", "/api/v1/adoptions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/available-slots").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/plans").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/newsletter/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contacto").permitAll()
                        .requestMatchers("/api/v1/ws/**").authenticated()
                        .requestMatchers("/payment/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(auth0JwtDecoder())
                                .jwtAuthenticationConverter(auth0JwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(jwtEntryPoint)
                );

        return http.build();
    }

    @Bean
    public JwtDecoder auth0JwtDecoder() {
        return new Auth0JwtDecoder(auth0IssuerUri, jwtSecret);
    }

    @Bean
    public Auth0JwtAuthenticationConverter auth0JwtAuthenticationConverter() {
        return new Auth0JwtAuthenticationConverter(usuarioRepository);
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        List<String> origins = appProperties.getCors().getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            // Validate: allowCredentials(true) + "*" origin is not allowed by browsers
            boolean hasWildcard = origins.contains("*");
            if (hasWildcard) {
                throw new IllegalStateException(
                        "CORS misconfiguration: allowedOrigins contains '*' with allowCredentials(true). "
                        + "Either remove '*' or set allowCredentials(false).");
            }
            configuration.setAllowedOrigins(origins);
        } else {
            configuration.addAllowedOrigin("http://localhost:5173");
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
