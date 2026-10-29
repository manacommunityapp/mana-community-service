package com.manacommunity.api.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
@RequiredArgsConstructor
public class OpsRequestMetricsFilter extends OncePerRequestFilter {

    private final org.springframework.beans.factory.ObjectProvider<OpsMetricsCollector> metricsCollectorProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/static") || uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String errorMessage = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String correlationId = (String) request.getAttribute("correlationId");
            if (correlationId == null) {
                correlationId = request.getHeader("X-Correlation-Id");
            }
            OpsMetricsCollector collector = metricsCollectorProvider.getIfAvailable();
            if (collector != null) {
                collector.recordRequest(
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration,
                        correlationId,
                        request.getRemoteAddr(),
                        errorMessage
                );
            }
        }
    }
}