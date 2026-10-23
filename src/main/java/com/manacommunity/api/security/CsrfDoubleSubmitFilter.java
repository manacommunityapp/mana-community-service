package com.manacommunity.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Double-submit cookie CSRF protection for web clients using cookie-based auth.
 *
 * <p>On login/register/refresh the server sets a non-HttpOnly {@code XSRF-TOKEN}
 * cookie (readable by JS). The frontend reads it and sends its value back as the
 * {@code X-XSRF-TOKEN} header on every state-changing request. This filter
 * compares the two: a mismatch or missing header rejects the request with 403.</p>
 *
 * <p>Only applies to web-platform requests that carry an auth cookie — mobile
 * clients using Bearer tokens are unaffected. Safe methods (GET, HEAD, OPTIONS)
 * are always allowed through.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class CsrfDoubleSubmitFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final CookieAuthHelper cookieAuthHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            chain.doFilter(request, response);
            return;
        }

        boolean hasAuthCookie = cookieAuthHelper.extractAccessToken(request) != null
                || cookieAuthHelper.extractRefreshToken(request) != null;

        if (!hasAuthCookie) {
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (uri.equals("/api/auth/login") || uri.equals("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String cookieToken = cookieAuthHelper.getCsrfCookie(request);
        String headerToken = cookieAuthHelper.getCsrfHeader(request);

        if (cookieToken == null || cookieToken.isBlank()
                || headerToken == null || headerToken.isBlank()
                || !cookieToken.equals(headerToken)) {
            log.warn("CSRF validation failed: uri={} ip={} cookiePresent={} headerPresent={}",
                    uri, request.getRemoteAddr(), cookieToken != null, headerToken != null);
            writeForbidden(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        String safePath = request.getRequestURI()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
        String body = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":403,"
                + "\"error\":\"CSRF_VALIDATION_FAILED\","
                + "\"message\":\"CSRF token is missing or invalid. Please refresh the page and try again.\","
                + "\"path\":\"" + safePath + "\"}";
        response.getWriter().write(body);
    }
}
