package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class CommercePermissions {

    private CommercePermissions() {}

    // ── Vendor Commerce Profile ─────────────────────────────────────────
    public static final String VIEW_COMMERCE_VENDORS      = "View Commerce Vendors";
    public static final String MANAGE_COMMERCE_VENDORS    = "Manage Commerce Vendors";
    public static final String VERIFY_COMMERCE_VENDOR     = "Verify Commerce Vendor";
    public static final String SETUP_COMMERCE_PROFILE     = "Setup Commerce Profile";

    // ── Commerce Products ───────────────────────────────────────────────
    public static final String VIEW_COMMERCE_PRODUCTS     = "View Commerce Products";
    public static final String MANAGE_COMMERCE_PRODUCTS   = "Manage Commerce Products";
    public static final String PUBLISH_COMMERCE_PRODUCT   = "Publish Commerce Product";

    // ── Commerce Orders ─────────────────────────────────────────────────
    public static final String VIEW_COMMERCE_ORDERS       = "View Commerce Orders";
    public static final String MANAGE_COMMERCE_ORDERS     = "Manage Commerce Orders";
    public static final String PROCESS_COMMERCE_ORDER     = "Process Commerce Order";

    // ── Commerce Inventory ──────────────────────────────────────────────
    public static final String VIEW_COMMERCE_INVENTORY    = "View Commerce Inventory";
    public static final String MANAGE_COMMERCE_INVENTORY  = "Manage Commerce Inventory";

    // ── Commerce Payments & Settlements ─────────────────────────────────
    public static final String VIEW_COMMERCE_PAYMENTS     = "View Commerce Payments";
    public static final String MANAGE_COMMERCE_PAYMENTS   = "Manage Commerce Payments";
    public static final String VIEW_COMMERCE_SETTLEMENTS  = "View Commerce Settlements";
    public static final String MANAGE_COMMERCE_SETTLEMENTS = "Manage Commerce Settlements";

    // ── Commerce Analytics ───────────────────────────────────────────────
    public static final String VIEW_COMMERCE_ANALYTICS    = "View Commerce Analytics";

    public static final List<String> ALL = List.of(
            VIEW_COMMERCE_VENDORS, MANAGE_COMMERCE_VENDORS, VERIFY_COMMERCE_VENDOR, SETUP_COMMERCE_PROFILE,
            VIEW_COMMERCE_PRODUCTS, MANAGE_COMMERCE_PRODUCTS, PUBLISH_COMMERCE_PRODUCT,
            VIEW_COMMERCE_ORDERS, MANAGE_COMMERCE_ORDERS, PROCESS_COMMERCE_ORDER,
            VIEW_COMMERCE_INVENTORY, MANAGE_COMMERCE_INVENTORY,
            VIEW_COMMERCE_PAYMENTS, MANAGE_COMMERCE_PAYMENTS,
            VIEW_COMMERCE_SETTLEMENTS, MANAGE_COMMERCE_SETTLEMENTS,
            VIEW_COMMERCE_ANALYTICS
    );
}
