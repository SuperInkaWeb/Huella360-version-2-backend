package com.vet_saas.config;

import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import com.vet_saas.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AppProperties appProperties;

    private final Map<Long, CachedUser> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300_000; // 5 minutos
    private static final int MAX_CACHE_SIZE = 100;

    private record CachedUser(Usuario usuario, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public WebSocketConfig(JwtService jwtService, UsuarioRepository usuarioRepository, AppProperties appProperties) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.appProperties = appProperties;

        // Limpiar entradas expiradas cada 5 minutos
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            userCache.entrySet().removeIf(e -> now - e.getValue().timestamp() > CACHE_TTL_MS);
        }, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Usar broker en memoria en todos los perfiles (ahorra ~100MB de Redis)
        // Con 1 instancia en plan $7 no necesitamos broker distribuido
        config.enableSimpleBroker("/topic", "/queue");
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
                                    // Limitar tamaño del cache
                                    if (userCache.size() >= MAX_CACHE_SIZE) {
                                        userCache.clear();
                                    }
                                    userCache.put(userId, new CachedUser(userDetails, System.currentTimeMillis()));
                                }
                            }

                            if (userDetails != null && jwtService.isTokenValid(token, userDetails)) {
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                log.debug("WS CONNECT autenticado para el usuario: {}", userDetails.getUsername());
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
}
