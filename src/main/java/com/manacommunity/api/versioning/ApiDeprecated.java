package com.manacommunity.api.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or whole controller) as deprecated.
 *
 * The DeprecationHeaderInterceptor reads this annotation and injects
 * RFC 8594 Deprecation + Sunset response headers so clients know
 * when this endpoint will be removed.
 *
 * Usage:
 *   {@code @ApiDeprecated(sunsetDate = "2026-06-01", replacedBy = "/api/v2/sports/events")}
 *   {@code @GetMapping("/api/v1/sports/events")}
 *   public ResponseEntity<?> listEventsV1(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDeprecated {
    /** ISO-8601 date after which this endpoint will be removed, e.g. "2026-06-01". */
    String sunsetDate();
    /** URI of the replacement endpoint. */
    String replacedBy() default "";
}
