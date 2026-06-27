package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a unique constraint is violated (duplicate email, phone, etc.). */
public class DuplicateResourceException extends ManaCommunityException {

    private final String diagnosticDetail;

    public DuplicateResourceException(String resource, String field, String value) {
        super("A " + resource + " with that " + field + " already exists.",
                HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
        this.diagnosticDetail = resource + " duplicate on " + field + ": " + mask(value);
    }

    public String getDiagnosticDetail() {
        return diagnosticDetail;
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 4) return "***";
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
