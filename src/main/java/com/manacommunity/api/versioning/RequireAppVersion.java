package com.manacommunity.api.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards a controller method behind a minimum client app version.
 *
 * When the caller's X-App-Version header is below {@code minVersion},
 * AppVersionGatingInterceptor returns HTTP 426 Upgrade Required with a
 * message telling the client to update.
 *
 * Version comparison is semver-style: "1.10.0" > "1.9.0".
 *
 * Usage:
 *   {@code @RequireAppVersion("2.5.0")}
 *   {@code @PostMapping("/api/sports/register-multi")}
 *   public ResponseEntity<?> batchRegister(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAppVersion {
    /** Minimum app version string in "MAJOR.MINOR.PATCH" format. */
    String value();
}
