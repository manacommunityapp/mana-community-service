package com.manacommunity.api.ai.config;

import io.micrometer.core.instrument.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Instruments AI chat requests with Prometheus metrics for Grafana dashboards.
 *
 * <p>Tracks:</p>
 * <ul>
 *   <li>{@code ai_chat_requests_total} — counter by status (success/error/rate_limited)</li>
 *   <li>{@code ai_chat_latency_seconds} — histogram of response times</li>
 *   <li>{@code ai_chat_active_requests} — gauge of in-flight requests</li>
 * </ul>
 *
 * <p>These integrate with the existing Prometheus/Grafana observability stack
 * configured in {@code observability-framework.md}.</p>
 */
@Slf4j
@Component
public class AiMetricsFilter implements Filter {

    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter rateLimitCounter;
    private final Timer latencyTimer;
    private final java.util.concurrent.atomic.AtomicInteger activeRequests;

    public AiMetricsFilter(MeterRegistry registry) {
        this.successCounter = Counter.builder("ai_chat_requests_total")
                .tag("status", "success")
                .description("Total successful AI chat requests")
                .register(registry);

        this.errorCounter = Counter.builder("ai_chat_requests_total")
                .tag("status", "error")
                .description("Total failed AI chat requests")
                .register(registry);

        this.rateLimitCounter = Counter.builder("ai_chat_requests_total")
                .tag("status", "rate_limited")
                .description("Total rate-limited AI chat requests")
                .register(registry);

        this.latencyTimer = Timer.builder("ai_chat_latency_seconds")
                .description("AI chat response latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);

        this.activeRequests = new java.util.concurrent.atomic.AtomicInteger(0);
        Gauge.builder("ai_chat_active_requests", activeRequests, java.util.concurrent.atomic.AtomicInteger::get)
                .description("Currently in-flight AI chat requests")
                .register(registry);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        if (!request.getRequestURI().startsWith("/api/ai/")
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        Instant start = Instant.now();
        activeRequests.incrementAndGet();

        try {
            chain.doFilter(req, res);

            HttpServletResponse response = (HttpServletResponse) res;
            int status = response.getStatus();

            if (status == 429) {
                rateLimitCounter.increment();
            } else if (status >= 200 && status < 300) {
                successCounter.increment();
            } else {
                errorCounter.increment();
            }
        } catch (Exception e) {
            errorCounter.increment();
            throw e;
        } finally {
            activeRequests.decrementAndGet();
            latencyTimer.record(Duration.between(start, Instant.now()));
        }
    }
}
