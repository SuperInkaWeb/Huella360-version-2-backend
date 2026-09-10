package com.vet_saas.config;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final AppProperties appProperties;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", appProperties.getExternal().getCloudinary().getCloudName());
        config.put("api_key", appProperties.getExternal().getCloudinary().getApiKey());
        config.put("api_secret", appProperties.getExternal().getCloudinary().getApiSecret());
        return new Cloudinary(config);
    }
}