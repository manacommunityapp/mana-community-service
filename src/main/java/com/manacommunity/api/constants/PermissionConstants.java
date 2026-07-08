package com.manacommunity.api.constants;

import java.util.List;
import java.util.stream.Stream;
import java.util.Collections;

/**
 * PermissionConstants — Single source of truth for all permission keys used in the system.
 * These values are stored in the `role_permissions.permission_key` column.
 *
 * <p>Usage: Import statically where needed.</p>
 * <pre>
 *   import static com.manacommunity.api.constants.PermissionConstants.*;
 * </pre>
 */
public final class PermissionConstants {

    private PermissionConstants() {
        // Non-instantiable utility class
    }

    // ──── ROLE NAMES ────────────────────────────────────────────────
    public static final String ROLE_SUPER_ADMIN    = "SUPER_ADMIN";
    public static final String ROLE_ADMIN          = "ADMIN";
    public static final String ROLE_COMMUNITY_ADMIN = "COMMUNITY_ADMIN";
    public static final String ROLE_SPORTS_ADMIN   = "SPORTS_ADMIN";
    public static final String ROLE_MEMBER         = "MEMBER";
    public static final String ROLE_VENDOR         = "VENDOR";
    public static final String ROLE_CASHIER        = "CASHIER";
    public static final String ROLE_STAFF          = "STAFF";

    // ──── COMMUNITY FEED ────────────────────────────────────────────
    public static final String VIEW_FEED       = "View Feed";
    public static final String CREATE_POST     = "Create Post";
    public static final String DELETE_POST     = "Delete Post";
    public static final String COMMENT_ON_POST = "Comment on Post";

    // ──── SPORTS — GRANULAR PERMISSIONS ─────────────────────────────
    // Main
    public static final String VIEW_SPORTS_MAIN          = "View Sports Main";
    public static final String CREATE_EDIT_SPORTS_MAIN   = "Create/Edit Sports Main";
    public static final String DELETE_SPORTS_MAIN        = "Delete Sports Main";
    // Sports Menu
    public static final String VIEW_SPORTS_MENU          = "View Sports Menu";
    public static final String CREATE_EDIT_SPORTS_MENU   = "Create/Edit Sports Menu";
    public static final String DELETE_SPORTS_MENU        = "Delete Sports Menu";
    // Auction Configuration
    public static final String VIEW_AUCTION_CONFIG       = "View Auction Configuration";
    public static final String CREATE_EDIT_AUCTION_CONFIG = "Create/Edit Auction Configuration";
    public static final String DELETE_AUCTION_CONFIG      = "Delete Auction Configuration";
    // Live Auction
    public static final String VIEW_LIVE_AUCTION         = "View Live Auction";
    public static final String CREATE_EDIT_LIVE_AUCTION  = "Create/Edit Live Auction";
    public static final String DELETE_LIVE_AUCTION       = "Delete Live Auction";
    // Teams Dashboard
    public static final String VIEW_TEAMS_DASHBOARD      = "View Teams Dashboard";
    public static final String CREATE_EDIT_TEAMS_DASHBOARD = "Create/Edit Teams Dashboard";
    public static final String DELETE_TEAMS_DASHBOARD    = "Delete Teams Dashboard";
    // Player Pool
    public static final String VIEW_PLAYER_POOL          = "View Player Pool";
    public static final String CREATE_EDIT_PLAYER_POOL   = "Create/Edit Player Pool";
    public static final String DELETE_PLAYER_POOL        = "Delete Player Pool";
    // Event Registrations
    public static final String VIEW_EVENT_REGISTRATIONS  = "View Event Registrations";
    public static final String CREATE_EDIT_EVENT_REGISTRATIONS = "Create/Edit Event Registrations";
    public static final String DELETE_EVENT_REGISTRATIONS = "Delete Event Registrations";
    // Auction Results
    public static final String VIEW_AUCTION_RESULTS      = "View Auction Results";
    public static final String CREATE_EDIT_AUCTION_RESULTS = "Create/Edit Auction Results";
    public static final String DELETE_AUCTION_RESULTS    = "Delete Auction Results";

    // ──── MARKETPLACE ───────────────────────────────────────────────
    public static final String VIEW_MARKETPLACE = "View Marketplace";
    public static final String CREATE_LISTING   = "Create Listing";
    public static final String DELETE_LISTING   = "Delete Listing";

    // ──── VISITOR / GATE PASS ──────────────────────────────────────
    public static final String VIEW_VISITORS      = "View Visitors";
    public static final String CREATE_VISITOR_PASS = "Create Visitor Pass";
    public static final String MANAGE_GATE        = "Manage Gate";

    // ──── JOBS & REFERRALS ──────────────────────────────────────────
    public static final String VIEW_JOBS  = "View Jobs";
    public static final String CREATE_JOB = "Create Job";
    public static final String APPLY_JOB  = "Apply Job";

    // ──── EVENTS ────────────────────────────────────────────────────
    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";

    // ──── ADMIN DASHBOARD ───────────────────────────────────────────
    public static final String VIEW_ADMIN         = "View Admin";
    public static final String VERIFY_KYC         = "Verify KYC";
    public static final String BULK_UPLOAD        = "Bulk Upload";
    public static final String MANAGE_COMMUNITIES = "Manage Communities";
    public static final String MANAGE_ROLES       = "Manage Roles";
    public static final String EDIT_VENUE_TIMING  = "Edit Venue Timing";

    // ──── SPORTS PERMISSION GROUPS ──────────────────────────────────
    /** All 8 View sports permissions */
    public static final List<String> ALL_SPORTS_VIEW_PERMISSIONS = List.of(
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, VIEW_AUCTION_RESULTS
    );

    /** All 24 sports permissions */
    public static final List<String> ALL_SPORTS_PERMISSIONS = List.of(
            VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN, DELETE_SPORTS_MAIN,
            VIEW_SPORTS_MENU, CREATE_EDIT_SPORTS_MENU, DELETE_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, CREATE_EDIT_AUCTION_CONFIG, DELETE_AUCTION_CONFIG,
            VIEW_LIVE_AUCTION, CREATE_EDIT_LIVE_AUCTION, DELETE_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, CREATE_EDIT_TEAMS_DASHBOARD, DELETE_TEAMS_DASHBOARD,
            VIEW_PLAYER_POOL, CREATE_EDIT_PLAYER_POOL, DELETE_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, CREATE_EDIT_EVENT_REGISTRATIONS, DELETE_EVENT_REGISTRATIONS,
            VIEW_AUCTION_RESULTS, CREATE_EDIT_AUCTION_RESULTS, DELETE_AUCTION_RESULTS
    );

    /**
     * Master list of ALL permission keys in the system.
     */
    public static final List<String> ALL_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_COMMUNITIES, MANAGE_ROLES, EDIT_VENUE_TIMING)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_ROLES, EDIT_VENUE_TIMING)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> SPORTS_ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS),
                    List.of(VIEW_JOBS),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, EDIT_VENUE_TIMING)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> MEMBER_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, VIEW_AUCTION_RESULTS,
            VIEW_MARKETPLACE,
            VIEW_VISITORS, CREATE_VISITOR_PASS,
            VIEW_JOBS, APPLY_JOB,
            VIEW_EVENTS, REGISTER_EVENT
    );

    public static final List<String> VENDOR_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
            VIEW_VISITORS,
            VIEW_JOBS, CREATE_JOB,
            VIEW_EVENTS, REGISTER_EVENT
    );

    public static final List<String> CASHIER_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS,
            VIEW_JOBS,
            VIEW_EVENTS
    );

    public static final List<String> STAFF_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS, MANAGE_GATE,
            VIEW_JOBS,
            VIEW_EVENTS
    );
}
