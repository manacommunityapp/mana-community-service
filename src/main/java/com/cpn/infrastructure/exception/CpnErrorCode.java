package com.cpn.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CpnErrorCode {
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    TENANT_NOT_FOUND("Tenant not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("Invalid credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token has expired", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("Email is already registered", HttpStatus.CONFLICT),
    PROFILE_NOT_FOUND("Profile not found", HttpStatus.NOT_FOUND),
    JOB_NOT_FOUND("Job not found", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus defaultStatus;

    CpnErrorCode(String defaultMessage, HttpStatus defaultStatus) {
        this.defaultMessage = defaultMessage;
        this.defaultStatus = defaultStatus;
    }
}
