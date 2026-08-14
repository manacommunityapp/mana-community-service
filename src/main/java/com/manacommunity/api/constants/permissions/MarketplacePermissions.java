package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class MarketplacePermissions {

    private MarketplacePermissions() {}

    public static final String VIEW_MARKETPLACE   = "View Marketplace";
    public static final String CREATE_LISTING     = "Create Listing";
    public static final String DELETE_LISTING     = "Delete Listing";
    public static final String MANAGE_MARKETPLACE = "Manage Marketplace";

    public static final List<String> ALL = List.of(
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING, MANAGE_MARKETPLACE
    );
}
