package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity (user, event, team, etc.) does not exist.
 * Sanitizes resource names to avoid disclosing internal database table or column names.
 */
public class ResourceNotFoundException extends ManaCommunityException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(ErrorSanitizer.cleanResourceName(resource) + " not found with id: " + id,
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, String id) {
        super(ErrorSanitizer.cleanResourceName(resource) + " not found with id: " + id,
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(ErrorSanitizer.cleanResourceName(resource) + " not found with the given "
                + ErrorSanitizer.cleanFieldName(field) + ".", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
