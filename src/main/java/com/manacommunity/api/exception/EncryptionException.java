package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when field-level PII encryption or decryption fails. */
public class EncryptionException extends ManaCommunityException {

    public EncryptionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "ENCRYPTION_ERROR");
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "ENCRYPTION_ERROR", cause);
    }
}
