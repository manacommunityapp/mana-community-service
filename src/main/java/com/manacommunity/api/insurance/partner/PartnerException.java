package com.manacommunity.api.insurance.partner;

/** Failure talking to a partner. {@code retryable} drives {@link PartnerCallExecutor} backoff. */
public class PartnerException extends Exception {
    private final String code;
    private final boolean retryable;

    public PartnerException(String code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public PartnerException(String code, String message, boolean retryable) {
        this(code, message, retryable, null);
    }

    public static PartnerException notSupported(String op) {
        return new PartnerException("NOT_SUPPORTED", op + " is not supported by this partner", false);
    }

    public String getCode() { return code; }
    public boolean isRetryable() { return retryable; }
}
