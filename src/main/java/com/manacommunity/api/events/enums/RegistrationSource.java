package com.manacommunity.api.events.enums;

public enum RegistrationSource {
    /** Created by the devotee themselves through the normal booking flow. */
    SELF,
    /** Created by an admin on behalf of a devotee (may have bypassed capacity/duplicate checks). */
    ADMIN,
    /** Bulk-imported by an admin (CSV / migration tool). */
    IMPORT
}
