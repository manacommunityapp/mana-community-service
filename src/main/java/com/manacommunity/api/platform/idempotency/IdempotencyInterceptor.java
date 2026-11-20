package com.manacommunity.api.platform.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Idempotency enforcement for POST/PUT/PATCH endpoints.
 *
 * Usage: clients include {@code Idempotency-Key: <client-uuid>} on mutating requests.
 * On retry with the same key, the interceptor replays the original response without
 * re-executing the handler. In-flight requests (key known but response not yet cached)
 * return 409 to prevent concurrent duplicate processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String ATTR_IDEMPOTENCY_KEY  = "idempotency_key_value";
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!IDEMPOTENT_METHODS.contains(request.getMethod())) {
            return true;
        }
        String keyValue = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (keyValue == null || keyValue.isBlank()) {
            return true;
        }

        Optional<IdempotencyKey> existing = repository.findById(keyValue.trim());
        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (key.getResponseBody() != null) {
                // Replay cached response
                response.setStatus(key.getResponseStatus());
                response.setContentType("application/json");
                response.setHeader("X-Idempotency-Replayed", "true");
                response.getWriter().write(key.getResponseBody());
                log.debug("Idempotency replay for key={} status={}", keyValue, key.getResponseStatus());
                return false;
            } else {
                // In-flight duplicate
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Duplicate request is still being processed\"}");
                return false;
            }
        }

        // Reserve the key slot with no response yet (in-flight guard)
        IdempotencyKey newKey = IdempotencyKey.builder()
                .keyValue(keyValue.trim())
                .requestPath(request.getRequestURI())
                .expiresAt(Instant.now().plusSeconds(86_400))
                .build();
        repository.save(newKey);
        request.setAttribute(ATTR_IDEMPOTENCY_KEY, keyValue.trim());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        String keyValue = (String) request.getAttribute(ATTR_IDEMPOTENCY_KEY);
        if (keyValue == null) return;

        // Only cache successful responses (2xx)
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            repository.deleteById(keyValue);
            return;
        }

        // Capture response body from wrapper if present
        String body = null;
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        }

        repository.findById(keyValue).ifPresent(key -> {
            key.setResponseStatus(response.getStatus());
            key.setResponseBody(body);
            repository.save(key);
        });
    }
}
