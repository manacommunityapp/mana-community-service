package com.manacommunity.api.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CookieAuthHelper {

    public static final String ACCESS_COOKIE  = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String CSRF_COOKIE    = "XSRF-TOKEN";
    public static final String CSRF_HEADER    = "X-XSRF-TOKEN";
    public static final String PLATFORM_HEADER = "X-Platform";
    public static final String PLATFORM_WEB    = "web";

    private final long accessMaxAgeSec;
    private final long refreshMaxAgeSec;
    private final boolean secureCookies;

    public CookieAuthHelper(
            @Value("${app.security.jwt.expiration-ms:900000}") long accessExpirationMs,
            @Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.security.cookie.secure:true}") boolean secureCookies) {
        this.accessMaxAgeSec  = accessExpirationMs / 1000;
        this.refreshMaxAgeSec = refreshExpirationMs / 1000;
        this.secureCookies    = secureCookies;
    }

    public boolean isWebPlatform(HttpServletRequest request) {
        return PLATFORM_WEB.equalsIgnoreCase(request.getHeader(PLATFORM_HEADER));
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_COOKIE, accessToken, accessMaxAgeSec, "/api");
        addCookie(response, REFRESH_COOKIE, refreshToken, refreshMaxAgeSec, "/api/auth");
        setCsrfCookie(response);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", 0, "/api");
        addCookie(response, REFRESH_COOKIE, "", 0, "/api/auth");
        clearCsrfCookie(response);
    }

    public void setCsrfCookie(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(secureCookies)
                .path("/")
                .maxAge(refreshMaxAgeSec)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCsrfCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE, "")
                .httpOnly(false)
                .secure(secureCookies)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .path(path)
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getCookieValue(HttpServletRequest request, String name) {
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
