package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class VendorPermissions {

    private VendorPermissions() {}

    public static final String VIEW_VENDOR_MANAGEMENT = "View Vendor Management";
    public static final String CREATE_VENDOR          = "Create Vendor";
    public static final String MANAGE_VENDORS         = "Manage Vendors";
    public static final String BOOK_VENDOR_SERVICE    = "Book Vendor Service";
    public static final String MANAGE_PROCUREMENT     = "Manage Procurement";
    public static final String MANAGE_CONTRACTS       = "Manage Contracts";
    public static final String MANAGE_VENDOR_PAYMENTS = "Manage Vendor Payments";
    public static final String RATE_VENDOR            = "Rate Vendor";
    public static final String VIEW_VENDOR_ANALYTICS  = "View Vendor Analytics";

    public static final List<String> ALL = List.of(
            VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS,
            BOOK_VENDOR_SERVICE, MANAGE_PROCUREMENT, MANAGE_CONTRACTS,
            MANAGE_VENDOR_PAYMENTS, RATE_VENDOR, VIEW_VENDOR_ANALYTICS
    );
}
