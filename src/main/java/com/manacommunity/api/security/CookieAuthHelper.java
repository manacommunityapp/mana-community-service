package com.manacommunity.api.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper to manage HttpOnly authentication cookies and CSRF double-submit tokens
 * for single-page applications.
 */
@Component
public class CookieAuthHelper {

    public static final String ACCESS_COOKIE   = "access_token";
    public static final String REFRESH_COOKIE  = "refresh_token";
    public static final String ACCESS_TOKEN_COOKIE  = ACCESS_COOKIE;
    public static final String REFRESH_TOKEN_COOKIE = REFRESH_COOKIE;
    public static final String CSRF_COOKIE     = "XSRF-TOKEN";
    public static final String CSRF_HEADER     = "X-XSRF-TOKEN";
    public static final String PLATFORM_HEADER = "X-Platform";
    public static final String PLATFORM_WEB    = "web";

    private final long accessMaxAgeSec;
    private final long refreshMaxAgeSec;
    private final boolean secureCookies;
    private final String cookieDomain;
    private final String sameSite;

    public CookieAuthHelper(
            @Value("${app.security.jwt.expiration-ms:86400000}") long accessExpirationMs,
            @Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.security.cookie.secure:${app.security.require-https:false}}") boolean secureCookies,
            @Value("${app.security.cookie-domain:}") String cookieDomain,
            @Value("${app.security.cookie.same-site:Lax}") String sameSite) {
        this.accessMaxAgeSec  = accessExpirationMs / 1000;
        this.refreshMaxAgeSec = refreshExpirationMs / 1000;
        this.secureCookies    = secureCookies;
        this.cookieDomain     = (cookieDomain != null && !cookieDomain.isBlank()) ? cookieDomain.trim() : null;
        this.sameSite         = sameSite;
    }

    public boolean isWebPlatform(HttpServletRequest request) {
        return PLATFORM_WEB.equalsIgnoreCase(request.getHeader(PLATFORM_HEADER));
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_COOKIE, accessToken, accessMaxAgeSec, "/");
        addCookie(response, REFRESH_COOKIE, refreshToken, refreshMaxAgeSec, "/");
        setCsrfCookie(response);
    }

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAuthCookies(response, accessToken, refreshToken);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", 0, "/");
        addCookie(response, REFRESH_COOKIE, "", 0, "/");
        clearCsrfCookie(response);
    }

    public void setCsrfCookie(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(secureCookies)
                .path("/")
                .maxAge(refreshMaxAgeSec)
                .sameSite(sameSite);

        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearCsrfCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(CSRF_COOKIE, "")
                .httpOnly(false)
                .secure(secureCookies)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite);

        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public String getCsrfCookie(HttpServletRequest request) {
        return getCookieValue(request, CSRF_COOKIE);
    }

    public String getCsrfHeader(HttpServletRequest request) {
        return request.getHeader(CSRF_HEADER);
    }

    public String extractAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_COOKIE);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value,
                           long maxAgeSec, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .path(path)
                .maxAge(maxAgeSec)
                .sameSite(sameSite);

        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        if (request == null) return null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                String val = c.getValue();
                return (val != null && !val.isBlank()) ? val : null;
            }
        }
        return null;
    }
}
