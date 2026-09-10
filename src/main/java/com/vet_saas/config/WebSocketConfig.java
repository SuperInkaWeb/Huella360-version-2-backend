package com.vet_saas.config;

import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AppProperties appProperties;

    private final Map<Long, CachedUser> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300_000; // 5 minutos

    private record CachedUser(Usuario usuario, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (isProductionProfile()) {
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(redisHost)
                    .setRelayPort(redisPort)
                    .setClientLogin(appProperties.getWebSocket().getClientLogin())
                    .setClientPasscode(appProperties.getWebSocket().getClientPasscode())
                    .setSystemLogin(appProperties.getWebSocket().getSystemLogin())
                    .setSystemPasscode(appProperties.getWebSocket().getSystemPasscode());
        } else {
            config.enableSimpleBroker("/topic", "/queue");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = appProperties.getCors().getAllowedOrigins();
        String[] allowedOrigins = origins != null && !origins.isEmpty()
                ? origins.toArray(new String[0])
                : new String[]{"http://localhost:5173"};

        registry.addEndpoint("/api/v1/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Long userId = jwtService.extractUserId(token);

                            CachedUser cached = userCache.get(userId);
                            Usuario userDetails;
                            if (cached != null && !cached.isExpired()) {
                                userDetails = cached.usuario();
                            } else {
                                userDetails = usuarioRepository.findById(userId).orElse(null);
                                if (userDetails != null) {
                                    userCache.put(userId, new CachedUser(userDetails, System.currentTimeMillis()));
                                }
                            }

                            if (userDetails != null && jwtService.isTokenValid(token, userDetails)) {
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                log.info("WS CONNECT autenticado para el usuario: {}", userDetails.getUsername());
                            } else {
                                log.warn("WS CONNECT rechazado: token inválido");
                                return null;
                            }
                        } catch (Exception e) {
                            log.error("WS CONNECT rechazado: error validando token", e);
                            return null;
                        }
                    } else {
                        log.warn("WS CONNECT rechazado: token de autenticación ausente");
                        return null;
                    }
                }
                return message;
            }
        });
    }

    private boolean isProductionProfile() {
        return activeProfile.contains("prod");
    }
}
