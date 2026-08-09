package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when file upload to cloud storage (AWS S3) fails.
 * Ensures strict two-phase semantics: metadata is ONLY saved to PostgreSQL
 * after S3 confirms successful object storage.
 */
public class MediaStorageException extends ManaCommunityException {

    public MediaStorageException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "MEDIA_STORAGE_ERROR");
    }

    public MediaStorageException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "MEDIA_STORAGE_ERROR", cause);
    }
}
