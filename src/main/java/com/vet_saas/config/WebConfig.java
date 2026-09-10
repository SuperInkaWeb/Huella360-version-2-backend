package com.vet_saas.config;

import com.vet_saas.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableSpringDataWebSupport
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = appProperties.getCors().getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            // Validate: allowCredentials(true) + "*" origin is not allowed by browsers
            boolean hasWildcard = origins.contains("*");
            if (hasWildcard) {
                throw new IllegalStateException(
                        "CORS misconfiguration: allowedOrigins contains '*' with allowCredentials(true). "
                        + "Either remove '*' or set allowCredentials(false).");
            }
        }
        String[] allowedOrigins = origins != null && !origins.isEmpty()
                ? origins.toArray(new String[0])
                : new String[]{"http://localhost:5173"};

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
