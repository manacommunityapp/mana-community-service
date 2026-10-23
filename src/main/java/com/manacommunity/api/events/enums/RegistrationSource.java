package com.manacommunity.api.events.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RegistrationSource {
    /** Created by the devotee themselves through the normal booking flow. */
    SELF,
    /** Created by an admin on behalf of a devotee (may have bypassed capacity/duplicate checks). */
    ADMIN,
    /** Bulk-imported by an admin (CSV / migration tool). */
    IMPORT;

    @JsonCreator
    public static RegistrationSource fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SELF;
        }
        String normalized = value.trim().toUpperCase();
        if ("USER".equals(normalized) || "DEVOTEE".equals(normalized) || "SELF".equals(normalized) || "MEMBER".equals(normalized)) {
            return SELF;
        }
        for (RegistrationSource source : values()) {
            if (source.name().equalsIgnoreCase(normalized)) {
                return source;
            }
        }
        return SELF;
    }
}
