package com.manacommunity.api.platform.web;

import com.manacommunity.api.security.CorrelationIdFilter;
import org.slf4j.MDC;

/**
 * Convenience accessor for the current request's correlation ID.
 * Reads directly from MDC so there is no duplicate ThreadLocal — the
 * existing {@link CorrelationIdFilter} already manages the lifecycle.
 */
public final class CorrelationContext {

    private CorrelationContext() {}

    public static String get() {
        return MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
    }
}
