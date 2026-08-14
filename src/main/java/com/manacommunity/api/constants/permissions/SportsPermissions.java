package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class SportsPermissions {

    private SportsPermissions() {}

    // Main
    public static final String VIEW_SPORTS_MAIN        = "View Sports Main";
    public static final String CREATE_EDIT_SPORTS_MAIN  = "Create/Edit Sports Main";
    public static final String DELETE_SPORTS_MAIN       = "Delete Sports Main";

    // Sports Menu
    public static final String VIEW_SPORTS_MENU        = "View Sports Menu";
    public static final String CREATE_EDIT_SPORTS_MENU  = "Create/Edit Sports Menu";
    public static final String DELETE_SPORTS_MENU       = "Delete Sports Menu";

    // Auction Configuration
    public static final String VIEW_AUCTION_CONFIG        = "View Auction Configuration";
    public static final String CREATE_EDIT_AUCTION_CONFIG  = "Create/Edit Auction Configuration";
    public static final String DELETE_AUCTION_CONFIG       = "Delete Auction Configuration";

    // Live Auction
    public static final String VIEW_LIVE_AUCTION        = "View Live Auction";
    public static final String CREATE_EDIT_LIVE_AUCTION  = "Create/Edit Live Auction";
    public static final String DELETE_LIVE_AUCTION       = "Delete Live Auction";

    // Teams Dashboard
    public static final String VIEW_TEAMS_DASHBOARD        = "View Teams Dashboard";
    public static final String CREATE_EDIT_TEAMS_DASHBOARD  = "Create/Edit Teams Dashboard";
    public static final String DELETE_TEAMS_DASHBOARD       = "Delete Teams Dashboard";

    // Player Pool
    public static final String VIEW_PLAYER_POOL        = "View Player Pool";
    public static final String CREATE_EDIT_PLAYER_POOL  = "Create/Edit Player Pool";
    public static final String DELETE_PLAYER_POOL       = "Delete Player Pool";

    // Event Registrations
    public static final String VIEW_EVENT_REGISTRATIONS        = "View Event Registrations";
    public static final String CREATE_EDIT_EVENT_REGISTRATIONS  = "Create/Edit Event Registrations";
    public static final String DELETE_EVENT_REGISTRATIONS       = "Delete Event Registrations";

    // Auction Results
    public static final String VIEW_AUCTION_RESULTS        = "View Auction Results";
    public static final String CREATE_EDIT_AUCTION_RESULTS  = "Create/Edit Auction Results";
    public static final String DELETE_AUCTION_RESULTS       = "Delete Auction Results";

    public static final List<String> VIEW_ALL = List.of(
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, VIEW_AUCTION_RESULTS
    );

    public static final List<String> ALL = List.of(
            VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN, DELETE_SPORTS_MAIN,
            VIEW_SPORTS_MENU, CREATE_EDIT_SPORTS_MENU, DELETE_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, CREATE_EDIT_AUCTION_CONFIG, DELETE_AUCTION_CONFIG,
            VIEW_LIVE_AUCTION, CREATE_EDIT_LIVE_AUCTION, DELETE_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, CREATE_EDIT_TEAMS_DASHBOARD, DELETE_TEAMS_DASHBOARD,
            VIEW_PLAYER_POOL, CREATE_EDIT_PLAYER_POOL, DELETE_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, CREATE_EDIT_EVENT_REGISTRATIONS, DELETE_EVENT_REGISTRATIONS,
            VIEW_AUCTION_RESULTS, CREATE_EDIT_AUCTION_RESULTS, DELETE_AUCTION_RESULTS
    );
}
