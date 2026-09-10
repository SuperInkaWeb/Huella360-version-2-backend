package com.vet_saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Auth0 auth0 = new Auth0();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private External external = new External();
    private Notification notification = new Notification();
    private Business business = new Business();
    private Ia ia = new Ia();
    private WebSocket webSocket = new WebSocket();

    @Data
    public static class Auth0 {
        private String issuer;
        private String audience;
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
        private long refreshExpiration;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class External {
        private CloudinaryProps cloudinary = new CloudinaryProps();
        private ApiPeruProps apiPeru = new ApiPeruProps();
        private MercadoPagoProps mercadoPago = new MercadoPagoProps();
        private MailProps mail = new MailProps();
        private ResendProps resend = new ResendProps();
        private String backendUrl;
        private String frontendUrl;
    }

    @Data
    public static class ResendProps {
        private String apiKey;
        private String fromEmail = "hola@huella360.com";
    }

    @Data
    public static class CloudinaryProps {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
    }

    @Data
    public static class ApiPeruProps {
        private String baseUrl;
        private String token;
    }

    @Data
    public static class MercadoPagoProps {
        private String accessToken;
        private String clientId;
        private String clientSecret;
        private String sandboxBuyerEmail;
        private boolean sandbox = false;
        private String webhookSecret;
    }

    @Data
    public static class MailProps {
        private String username;
        private String fromEmail;
    }

    @Data
    public static class Notification {
        private String adminEmail = "admin@huella360.com";
    }

    @Data
    public static class Business {
        private BigDecimal commissionPercentage = new BigDecimal("0.05");
        private String defaultCountry = "Peru";
        private String defaultCurrency = "PEN";
        private String defaultPlanName = "Huella Free B2B";
    }

    @Data
    public static class Ia {
        private String openaiApiKey;
        private String openaiModel = "gpt-4o-mini";
        private String groqApiKey;
        private String groqModel = "llama-3.3-70b-versatile";
    }

    @Data
    public static class WebSocket {
        private String clientLogin = "guest";
        private String clientPasscode = "guest";
        private String systemLogin = "guest";
        private String systemPasscode = "guest";
    }
}
