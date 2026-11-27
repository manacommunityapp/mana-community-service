package com.manacommunity.api.social.privacy.service;

import com.manacommunity.api.social.privacy.model.*;

import java.util.List;

public interface DirectoryPrivacyEngine {

    /**
     * Registers or updates a resident's privacy matrix settings.
     */
    void registerPrivacyMatrix(UserPrivacyMatrix matrix);

    /**
     * Sanitizes a single raw directory record based on the viewer's context and target's privacy settings.
     */
    SanitizedDirectoryEntry sanitizeRecord(ViewerContext viewer, ResidentDirectoryEntry target);

    /**
     * Batch sanitizes search results in O(1) time per record using in-memory set intersections.
     */
    List<SanitizedDirectoryEntry> sanitizeBatch(ViewerContext viewer, List<ResidentDirectoryEntry> targets);

    /**
     * Evaluates if a viewer has permission to view a specific attribute of a target user.
     */
    boolean canViewField(ViewerContext viewer, ResidentDirectoryEntry target, VisibilityScope scope);

    /**
     * Standard phone number masker: +919876543210 -> +91 98*** **210
     */
    String maskPhoneNumber(String rawPhone);

    /**
     * Standard vehicle number plate masker: KA01AB1234 -> KA 01 ** 1234
     */
    String maskVehicleNumber(String rawPlate);
}
