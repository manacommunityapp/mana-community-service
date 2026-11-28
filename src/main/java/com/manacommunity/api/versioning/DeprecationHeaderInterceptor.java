package com.manacommunity.api.versioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Injects RFC 8594 Deprecation + Sunset headers on any endpoint annotated
 * with @ApiDeprecated.
 *
 * Example response headers:
 *   Deprecation: true
 *   Sunset: Sun, 01 Jun 2026 00:00:00 GMT
 *   Link: </api/v2/sports/events>; rel="successor-version"
 *
 * Register this interceptor in WebMvcConfig.addInterceptors().
 */
@Component
public class DeprecationHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        if (!(handler instanceof HandlerMethod hm)) return true;

        ApiDeprecated ann = AnnotationUtils.findAnnotation(hm.getMethod(), ApiDeprecated.class);
        if (ann == null) {
            ann = AnnotationUtils.findAnnotation(hm.getBeanType(), ApiDeprecated.class);
        }
        if (ann == null) return true;

        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", toHttpDate(ann.sunsetDate()));
        if (!ann.replacedBy().isBlank()) {
            response.setHeader("Link", "<" + ann.replacedBy() + ">; rel=\"successor-version\"");
        }
        return true;
    }

    private String toHttpDate(String isoDate) {
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(isoDate);
            java.time.ZonedDateTime zdt = d.atStartOfDay(java.time.ZoneOffset.UTC);
            return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(zdt);
        } catch (Exception e) {
            return isoDate;
        }
    }
}
