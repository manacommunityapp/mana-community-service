package com.manacommunity.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Normalizes incoming HTTP request URIs that contain duplicate "/api" prefixes
 * (such as "/api/api/v1/emergency/sos/trigger" or "/api/api/notices").
 *
 * <p>This commonly occurs when mobile or web API clients have their base URL set to
 * {@code ${API_BASE_URL}/api} and client service methods also pass paths starting with {@code /api/...}.
 *
 * <p>Ordered with {@code HIGHEST_PRECEDENCE + 5} so that all subsequent filters (including
 * Spring Security's filter chain and DispatcherServlet) see the cleanly normalized URI.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class DuplicateApiPrefixFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DuplicateApiPrefixFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/api/") || uri.equals("/api/api"))) {
            String normalizedUri = normalizeUri(uri);
            log.warn("Detected duplicate /api prefix on incoming request: '{}'. Normalizing to '{}'", uri, normalizedUri);
            HttpServletRequest wrappedRequest = new NormalizedUriRequestWrapper(request, normalizedUri);
            chain.doFilter(wrappedRequest, response);
            return;
        }

        chain.doFilter(request, response);
    }

    public static String normalizeUri(String uri) {
        if (uri == null) {
            return null;
        }
        String current = uri;
        while (current.startsWith("/api/api/") || current.equals("/api/api")) {
            if (current.equals("/api/api")) {
                current = "/api";
            } else {
                current = current.substring(4); // strip first "/api"
            }
        }
        return current;
    }

    private static class NormalizedUriRequestWrapper extends HttpServletRequestWrapper {
        private final String normalizedUri;

        public NormalizedUriRequestWrapper(HttpServletRequest request, String normalizedUri) {
            super(request);
            this.normalizedUri = normalizedUri;
        }

        @Override
        public String getRequestURI() {
            return normalizedUri;
        }

        @Override
        public String getServletPath() {
            return normalizedUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            String scheme = getScheme();
            int port = getServerPort();
            url.append(scheme).append("://").append(getServerName());
            if ((scheme.equalsIgnoreCase("http") && port != 80 && port > 0)
                    || (scheme.equalsIgnoreCase("https") && port != 443 && port > 0)) {
                url.append(':').append(port);
            }
            url.append(normalizedUri);
            return url;
        }
    }
}
