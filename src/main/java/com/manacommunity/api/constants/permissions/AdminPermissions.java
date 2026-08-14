package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class AdminPermissions {

    private AdminPermissions() {}

    public static final String VIEW_ADMIN         = "View Admin";
    public static final String VERIFY_KYC         = "Verify KYC";
    public static final String BULK_UPLOAD        = "Bulk Upload";
    public static final String MANAGE_COMMUNITIES = "Manage Communities";
    public static final String MANAGE_ROLES       = "Manage Roles";
    public static final String EDIT_VENUE_TIMING  = "Edit Venue Timing";

    public static final List<String> ALL = List.of(
            VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD,
            MANAGE_COMMUNITIES, MANAGE_ROLES, EDIT_VENUE_TIMING
    );
}
