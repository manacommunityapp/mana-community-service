package com.manacommunity.api.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards a controller method behind a community module flag.
 *
 * Usage:
 *   {@code @ModuleRequired("SPORTS")}
 *   {@code @GetMapping("/api/sports/events")}
 *   public ResponseEntity<?> listEvents(...) { ... }
 *
 * The aspect reads the community from the authenticated UserPrincipal and
 * throws ModuleNotEnabledException (HTTP 403) when the module is off.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRequired {
    String value();
}
