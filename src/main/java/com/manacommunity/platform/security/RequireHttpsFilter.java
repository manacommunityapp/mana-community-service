package com.manacommunity.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * When {@code APP_REQUIRE_HTTPS=true}, rejects plain-HTTP requests with a 301 redirect.
 * Works in conjunction with Spring's ForwardedHeaderFilter so that proxied requests
 * behind a load balancer are evaluated correctly.
 * Health-check paths (ALB/NLB) are exempt so the load balancer never sees a redirect.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequireHttpsFilter extends OncePerRequestFilter {

    private final boolean requireHttps;

    public RequireHttpsFilter(
            @Value("${APP_REQUIRE_HTTPS:false}") boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!requireHttps) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Exempt ALB/NLB health-check paths from the redirect
        if (path.equals("/actuator/health") || path.equals("/health")) {
            chain.doFilter(request, response);
            return;
        }

        // ForwardedHeaderFilter (registered as a bean elsewhere) has already rewritten
        // the scheme from X-Forwarded-Proto before this filter runs.
        if ("https".equalsIgnoreCase(request.getScheme())) {
            chain.doFilter(request, response);
            return;
        }

        String host = request.getServerName();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String redirectUrl = "https://" + host + uri + (query != null ? "?" + query : "");

        log.debug("Redirecting HTTP request to HTTPS: {}", redirectUrl);
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", redirectUrl);
    }
}
