package com.manacommunity.api.common.idempotency.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint or service operation as idempotent.
 * Intercepts Idempotency-Key headers to guarantee safe deduplication
 * and identical response playback.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** Time-to-live for cached idempotency responses in minutes (default 24 hours). */
    int ttlMinutes() default 1440;

    /** Whether the Idempotency-Key header is strictly required. */
    boolean required() default false;
}
