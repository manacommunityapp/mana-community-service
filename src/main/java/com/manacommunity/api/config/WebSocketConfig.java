package com.manacommunity.api.config;

import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.security.StompSubscriptionInterceptor;
import com.manacommunity.platform.websocket.StompSecurityChannelInterceptor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * STOMP-over-WebSocket configuration backing real-time chat (and any future
 * live push, e.g. the live auction).
 *
 * <ul>
 *   <li>Handshake endpoint: {@code /ws} (native WebSocket — no SockJS, so the
 *       browser client connects with a plain {@code ws://} URL).</li>
 *   <li>Simple in-memory broker on {@code /topic}; client sends to {@code /app}.</li>
 *   <li>A CONNECT-frame interceptor authenticates the STOMP session from the
 *       {@code Authorization: Bearer <jwt>} header (same tokens as the REST API,
 *       plus the dev {@code mock-token-<id>} convenience).</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    private final StompSubscriptionInterceptor stompSubscriptionInterceptor;
    private final StompSecurityChannelInterceptor stompSecurityChannelInterceptor;
    private ThreadPoolTaskScheduler heartbeatScheduler;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Value("${app.security.mock-auth-enabled:false}")
    private boolean mockAuthEnabled;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider, Environment environment,
                           StompSubscriptionInterceptor stompSubscriptionInterceptor,
                           StompSecurityChannelInterceptor stompSecurityChannelInterceptor) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
        this.stompSubscriptionInterceptor = stompSubscriptionInterceptor;
        this.stompSecurityChannelInterceptor = stompSecurityChannelInterceptor;
    }

    @PostConstruct
    void validateMockAuthProfile() {
        if (!mockAuthEnabled) return;
        boolean isLocal = java.util.Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (!isLocal) {
            throw new IllegalStateException(
                    "WebSocket mock-auth is only allowed with the 'local' profile.");
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        this.heartbeatScheduler = scheduler;

        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(scheduler)
                .setHeartbeatValue(new long[]{10000L, 10000L});
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @PreDestroy
    void shutdownHeartbeatScheduler() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                }
                return message;
            }
        }, stompSubscriptionInterceptor, stompSecurityChannelInterceptor);
    }

    /** Resolves a user id from the CONNECT Authorization header and attaches it as the session principal. */
    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7).trim();
        Long userId = null;
        if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
            userId = jwtTokenProvider.getUserId(token);
        } else if (mockAuthEnabled && token.startsWith("mock-token-")) {
            try {
                userId = Long.parseLong(token.substring("mock-token-".length()));
            } catch (NumberFormatException ignored) {
                // leave unauthenticated
            }
        }
        if (userId != null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId), null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            accessor.setUser(auth);
        }
    }
}
