package com.manacommunity.api.mw.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adds informational X-RateLimit-* headers to public MW endpoint responses.
 * Actual enforcement is done by nginx (limit_req_zone); this filter only
 * tells clients what limits apply so they can back off gracefully.
 */
@Component
@Order(10)
public class MwPublicRateLimitHeaderFilter implements Filter {

    private static final String[] PUBLIC_PREFIXES = {
        "/api/v1/mw/jobs",
        "/api/v1/mw/pool/browse",
        "/api/v1/mw/passport/"
    };

    private static final String[] APPLY_SUFFIXES = {
        "/apply"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        if (isApplyEndpoint(uri)) {
            response.setHeader("X-RateLimit-Limit",  "5");
            response.setHeader("X-RateLimit-Window", "60s");
            response.setHeader("X-RateLimit-Scope",  "per-ip");
        } else if (isPublicEndpoint(uri)) {
            response.setHeader("X-RateLimit-Limit",  "10");
            response.setHeader("X-RateLimit-Window", "60s");
            response.setHeader("X-RateLimit-Scope",  "per-ip");
        }

        chain.doFilter(req, res);
    }

    private boolean isPublicEndpoint(String uri) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean isApplyEndpoint(String uri) {
        return uri.startsWith("/api/v1/mw/") && uri.endsWith("/apply");
    }
}
