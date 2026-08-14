package com.manacommunity.api.constants;

import com.manacommunity.api.constants.permissions.*;

import java.util.List;
import java.util.stream.Stream;
import java.util.Collections;

public final class PermissionConstants {

    private PermissionConstants() {}

    // ──── ROLE NAMES ────────────────────────────────────────────────
    public static final String ROLE_SUPER_ADMIN    = "SUPER_ADMIN";
    public static final String ROLE_ADMIN          = "ADMIN";
    public static final String ROLE_COMMUNITY_ADMIN = "COMMUNITY_ADMIN";
    public static final String ROLE_SPORTS_ADMIN   = "SPORTS_ADMIN";
    public static final String ROLE_MEMBER         = "MEMBER";
    public static final String ROLE_VENDOR         = "VENDOR";
    public static final String ROLE_CASHIER        = "CASHIER";
    public static final String ROLE_STAFF          = "STAFF";

    // ──── RE-EXPORTS: Community Feed ────────────────────────────────
    public static final String VIEW_FEED       = CommunityFeedPermissions.VIEW_FEED;
    public static final String CREATE_POST     = CommunityFeedPermissions.CREATE_POST;
    public static final String DELETE_POST     = CommunityFeedPermissions.DELETE_POST;
    public static final String COMMENT_ON_POST = CommunityFeedPermissions.COMMENT_ON_POST;

    // ──── RE-EXPORTS: Sports ────────────────────────────────────────
    public static final String VIEW_SPORTS_MAIN               = SportsPermissions.VIEW_SPORTS_MAIN;
    public static final String CREATE_EDIT_SPORTS_MAIN        = SportsPermissions.CREATE_EDIT_SPORTS_MAIN;
    public static final String DELETE_SPORTS_MAIN             = SportsPermissions.DELETE_SPORTS_MAIN;
    public static final String VIEW_SPORTS_MENU               = SportsPermissions.VIEW_SPORTS_MENU;
    public static final String CREATE_EDIT_SPORTS_MENU        = SportsPermissions.CREATE_EDIT_SPORTS_MENU;
    public static final String DELETE_SPORTS_MENU             = SportsPermissions.DELETE_SPORTS_MENU;
    public static final String VIEW_AUCTION_CONFIG            = SportsPermissions.VIEW_AUCTION_CONFIG;
    public static final String CREATE_EDIT_AUCTION_CONFIG     = SportsPermissions.CREATE_EDIT_AUCTION_CONFIG;
    public static final String DELETE_AUCTION_CONFIG          = SportsPermissions.DELETE_AUCTION_CONFIG;
    public static final String VIEW_LIVE_AUCTION              = SportsPermissions.VIEW_LIVE_AUCTION;
    public static final String CREATE_EDIT_LIVE_AUCTION       = SportsPermissions.CREATE_EDIT_LIVE_AUCTION;
    public static final String DELETE_LIVE_AUCTION            = SportsPermissions.DELETE_LIVE_AUCTION;
    public static final String VIEW_TEAMS_DASHBOARD           = SportsPermissions.VIEW_TEAMS_DASHBOARD;
    public static final String CREATE_EDIT_TEAMS_DASHBOARD    = SportsPermissions.CREATE_EDIT_TEAMS_DASHBOARD;
    public static final String DELETE_TEAMS_DASHBOARD         = SportsPermissions.DELETE_TEAMS_DASHBOARD;
    public static final String VIEW_PLAYER_POOL               = SportsPermissions.VIEW_PLAYER_POOL;
    public static final String CREATE_EDIT_PLAYER_POOL        = SportsPermissions.CREATE_EDIT_PLAYER_POOL;
    public static final String DELETE_PLAYER_POOL             = SportsPermissions.DELETE_PLAYER_POOL;
    public static final String VIEW_EVENT_REGISTRATIONS       = SportsPermissions.VIEW_EVENT_REGISTRATIONS;
    public static final String CREATE_EDIT_EVENT_REGISTRATIONS = SportsPermissions.CREATE_EDIT_EVENT_REGISTRATIONS;
    public static final String DELETE_EVENT_REGISTRATIONS     = SportsPermissions.DELETE_EVENT_REGISTRATIONS;
    public static final String VIEW_AUCTION_RESULTS           = SportsPermissions.VIEW_AUCTION_RESULTS;
    public static final String CREATE_EDIT_AUCTION_RESULTS    = SportsPermissions.CREATE_EDIT_AUCTION_RESULTS;
    public static final String DELETE_AUCTION_RESULTS         = SportsPermissions.DELETE_AUCTION_RESULTS;

    // ──── RE-EXPORTS: Marketplace ───────────────────────────────────
    public static final String VIEW_MARKETPLACE   = MarketplacePermissions.VIEW_MARKETPLACE;
    public static final String CREATE_LISTING     = MarketplacePermissions.CREATE_LISTING;
    public static final String DELETE_LISTING     = MarketplacePermissions.DELETE_LISTING;
    public static final String MANAGE_MARKETPLACE = MarketplacePermissions.MANAGE_MARKETPLACE;

    // ──── RE-EXPORTS: Visitor / Gate Pass ───────────────────────────
    public static final String VIEW_VISITORS      = VisitorPermissions.VIEW_VISITORS;
    public static final String CREATE_VISITOR_PASS = VisitorPermissions.CREATE_VISITOR_PASS;
    public static final String MANAGE_GATE        = VisitorPermissions.MANAGE_GATE;

    // ──── RE-EXPORTS: Amenity Booking ───────────────────────────────
    public static final String VIEW_AMENITIES   = AmenityPermissions.VIEW_AMENITIES;
    public static final String BOOK_AMENITY     = AmenityPermissions.BOOK_AMENITY;
    public static final String MANAGE_AMENITIES = AmenityPermissions.MANAGE_AMENITIES;

    // ──── RE-EXPORTS: Notice Board ──────────────────────────────────
    public static final String VIEW_NOTICES  = NoticeBoardPermissions.VIEW_NOTICES;
    public static final String CREATE_NOTICE = NoticeBoardPermissions.CREATE_NOTICE;
    public static final String DELETE_NOTICE = NoticeBoardPermissions.DELETE_NOTICE;

    // ──── RE-EXPORTS: Helpdesk / Complaints ─────────────────────────
    public static final String VIEW_TICKETS   = HelpdeskPermissions.VIEW_TICKETS;
    public static final String CREATE_TICKET  = HelpdeskPermissions.CREATE_TICKET;
    public static final String MANAGE_TICKETS = HelpdeskPermissions.MANAGE_TICKETS;

    // ──── RE-EXPORTS: Polling / Voting ──────────────────────────────
    public static final String VIEW_POLLS  = PollingPermissions.VIEW_POLLS;
    public static final String CREATE_POLL = PollingPermissions.CREATE_POLL;
    public static final String VOTE_POLL   = PollingPermissions.VOTE_POLL;

    // ──── RE-EXPORTS: Jobs & Referrals ──────────────────────────────
    public static final String VIEW_JOBS  = JobsPermissions.VIEW_JOBS;
    public static final String CREATE_JOB = JobsPermissions.CREATE_JOB;
    public static final String APPLY_JOB  = JobsPermissions.APPLY_JOB;

    // ──── RE-EXPORTS: Events ────────────────────────────────────────
    public static final String VIEW_EVENTS    = EventPermissions.VIEW_EVENTS;
    public static final String CREATE_EVENT   = EventPermissions.CREATE_EVENT;
    public static final String REGISTER_EVENT = EventPermissions.REGISTER_EVENT;

    // ──── RE-EXPORTS: Service Platform ──────────────────────────────
    public static final String VIEW_SERVICE_CATALOG     = ServicePlatformPermissions.VIEW_SERVICE_CATALOG;
    public static final String MANAGE_SERVICE_CATALOG   = ServicePlatformPermissions.MANAGE_SERVICE_CATALOG;
    public static final String VIEW_SERVICE_PROVIDERS   = ServicePlatformPermissions.VIEW_SERVICE_PROVIDERS;
    public static final String MANAGE_SERVICE_PROVIDERS = ServicePlatformPermissions.MANAGE_SERVICE_PROVIDERS;
    public static final String CREATE_SERVICE_REQUEST   = ServicePlatformPermissions.CREATE_SERVICE_REQUEST;
    public static final String VIEW_SERVICE_REQUESTS    = ServicePlatformPermissions.VIEW_SERVICE_REQUESTS;
    public static final String MANAGE_SERVICE_REQUESTS  = ServicePlatformPermissions.MANAGE_SERVICE_REQUESTS;
    public static final String VIEW_WORK_ORDERS         = ServicePlatformPermissions.VIEW_WORK_ORDERS;
    public static final String MANAGE_WORK_ORDERS       = ServicePlatformPermissions.MANAGE_WORK_ORDERS;

    // ──── RE-EXPORTS: Vendor Management ─────────────────────────────
    public static final String VIEW_VENDOR_MANAGEMENT = VendorPermissions.VIEW_VENDOR_MANAGEMENT;
    public static final String CREATE_VENDOR          = VendorPermissions.CREATE_VENDOR;
    public static final String MANAGE_VENDORS         = VendorPermissions.MANAGE_VENDORS;
    public static final String BOOK_VENDOR_SERVICE    = VendorPermissions.BOOK_VENDOR_SERVICE;
    public static final String MANAGE_PROCUREMENT     = VendorPermissions.MANAGE_PROCUREMENT;
    public static final String MANAGE_CONTRACTS       = VendorPermissions.MANAGE_CONTRACTS;
    public static final String MANAGE_VENDOR_PAYMENTS = VendorPermissions.MANAGE_VENDOR_PAYMENTS;
    public static final String RATE_VENDOR            = VendorPermissions.RATE_VENDOR;
    public static final String VIEW_VENDOR_ANALYTICS  = VendorPermissions.VIEW_VENDOR_ANALYTICS;

    // ──── RE-EXPORTS: Food & Lifestyle OS ───────────────────────────
    public static final String VIEW_FOOD_PROFILE            = FoodPermissions.VIEW_FOOD_PROFILE;
    public static final String MANAGE_FOOD_PROFILE          = FoodPermissions.MANAGE_FOOD_PROFILE;
    public static final String VIEW_FOOD_RESTAURANTS        = FoodPermissions.VIEW_FOOD_RESTAURANTS;
    public static final String MANAGE_FOOD_RESTAURANTS      = FoodPermissions.MANAGE_FOOD_RESTAURANTS;
    public static final String VIEW_FOOD_MENU               = FoodPermissions.VIEW_FOOD_MENU;
    public static final String MANAGE_FOOD_MENU             = FoodPermissions.MANAGE_FOOD_MENU;
    public static final String VIEW_FOOD_HOME_CHEFS         = FoodPermissions.VIEW_FOOD_HOME_CHEFS;
    public static final String MANAGE_FOOD_HOME_CHEFS       = FoodPermissions.MANAGE_FOOD_HOME_CHEFS;
    public static final String VIEW_FOOD_ORDERS             = FoodPermissions.VIEW_FOOD_ORDERS;
    public static final String MANAGE_FOOD_ORDERS           = FoodPermissions.MANAGE_FOOD_ORDERS;
    public static final String VIEW_FOOD_SUBSCRIPTIONS      = FoodPermissions.VIEW_FOOD_SUBSCRIPTIONS;
    public static final String MANAGE_FOOD_SUBSCRIPTIONS    = FoodPermissions.MANAGE_FOOD_SUBSCRIPTIONS;
    public static final String VIEW_FOOD_DINING             = FoodPermissions.VIEW_FOOD_DINING;
    public static final String MANAGE_FOOD_DINING           = FoodPermissions.MANAGE_FOOD_DINING;
    public static final String VIEW_FOOD_GROCERY            = FoodPermissions.VIEW_FOOD_GROCERY;
    public static final String MANAGE_FOOD_GROCERY          = FoodPermissions.MANAGE_FOOD_GROCERY;
    public static final String VIEW_FOOD_RECIPES            = FoodPermissions.VIEW_FOOD_RECIPES;
    public static final String MANAGE_FOOD_RECIPES          = FoodPermissions.MANAGE_FOOD_RECIPES;
    public static final String VIEW_FOOD_NUTRITION          = FoodPermissions.VIEW_FOOD_NUTRITION;
    public static final String MANAGE_FOOD_NUTRITION        = FoodPermissions.MANAGE_FOOD_NUTRITION;
    public static final String VIEW_FOOD_DELIVERY           = FoodPermissions.VIEW_FOOD_DELIVERY;
    public static final String MANAGE_FOOD_DELIVERY         = FoodPermissions.MANAGE_FOOD_DELIVERY;
    public static final String VIEW_FOOD_COMMUNITY_KITCHEN  = FoodPermissions.VIEW_FOOD_COMMUNITY_KITCHEN;
    public static final String MANAGE_FOOD_COMMUNITY_KITCHEN = FoodPermissions.MANAGE_FOOD_COMMUNITY_KITCHEN;
    public static final String VIEW_FOOD_CATERING           = FoodPermissions.VIEW_FOOD_CATERING;
    public static final String MANAGE_FOOD_CATERING         = FoodPermissions.MANAGE_FOOD_CATERING;
    public static final String VIEW_FOOD_CORPORATE          = FoodPermissions.VIEW_FOOD_CORPORATE;
    public static final String MANAGE_FOOD_CORPORATE        = FoodPermissions.MANAGE_FOOD_CORPORATE;
    public static final String VIEW_FOOD_EVENTS             = FoodPermissions.VIEW_FOOD_EVENTS;
    public static final String MANAGE_FOOD_EVENTS           = FoodPermissions.MANAGE_FOOD_EVENTS;
    public static final String VIEW_FOOD_PANTRY             = FoodPermissions.VIEW_FOOD_PANTRY;
    public static final String MANAGE_FOOD_PANTRY           = FoodPermissions.MANAGE_FOOD_PANTRY;
    public static final String VIEW_FOOD_LOYALTY            = FoodPermissions.VIEW_FOOD_LOYALTY;
    public static final String MANAGE_FOOD_LOYALTY          = FoodPermissions.MANAGE_FOOD_LOYALTY;
    public static final String VIEW_FOOD_ANALYTICS          = FoodPermissions.VIEW_FOOD_ANALYTICS;
    public static final String VIEW_FOOD_PAYMENTS           = FoodPermissions.VIEW_FOOD_PAYMENTS;
    public static final String MANAGE_FOOD_PAYMENTS         = FoodPermissions.MANAGE_FOOD_PAYMENTS;
    public static final String VIEW_FOOD_CLOUD_KITCHENS     = FoodPermissions.VIEW_FOOD_CLOUD_KITCHENS;
    public static final String MANAGE_FOOD_CLOUD_KITCHENS   = FoodPermissions.MANAGE_FOOD_CLOUD_KITCHENS;

    // ──── RE-EXPORTS: Admin Dashboard ───────────────────────────────
    public static final String VIEW_ADMIN         = AdminPermissions.VIEW_ADMIN;
    public static final String VERIFY_KYC         = AdminPermissions.VERIFY_KYC;
    public static final String BULK_UPLOAD        = AdminPermissions.BULK_UPLOAD;
    public static final String MANAGE_COMMUNITIES = AdminPermissions.MANAGE_COMMUNITIES;
    public static final String MANAGE_ROLES       = AdminPermissions.MANAGE_ROLES;
    public static final String EDIT_VENUE_TIMING  = AdminPermissions.EDIT_VENUE_TIMING;

    // ──── SPORTS PERMISSION GROUPS (delegates) ──────────────────────
    public static final List<String> ALL_SPORTS_VIEW_PERMISSIONS = SportsPermissions.VIEW_ALL;
    public static final List<String> ALL_SPORTS_PERMISSIONS      = SportsPermissions.ALL;

    // ──── MASTER LIST ───────────────────────────────────────────────
    public static final List<String> ALL_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    CommunityFeedPermissions.ALL,
                    SportsPermissions.ALL,
                    MarketplacePermissions.ALL,
                    VisitorPermissions.ALL,
                    AmenityPermissions.ALL,
                    NoticeBoardPermissions.ALL,
                    HelpdeskPermissions.ALL,
                    PollingPermissions.ALL,
                    JobsPermissions.ALL,
                    EventPermissions.ALL,
                    AdminPermissions.ALL,
                    ServicePlatformPermissions.ALL,
                    VendorPermissions.ALL
            ).flatMap(List::stream).distinct().toList()
    );

    // ──── ROLE → PERMISSION LISTS ───────────────────────────────────

    public static final List<String> ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    CommunityFeedPermissions.ALL,
                    SportsPermissions.ALL,
                    MarketplacePermissions.ALL,
                    VisitorPermissions.ALL,
                    AmenityPermissions.ALL,
                    NoticeBoardPermissions.ALL,
                    HelpdeskPermissions.ALL,
                    PollingPermissions.ALL,
                    JobsPermissions.ALL,
                    EventPermissions.ALL,
                    List.of(AdminPermissions.VIEW_ADMIN, AdminPermissions.VERIFY_KYC,
                            AdminPermissions.BULK_UPLOAD, AdminPermissions.MANAGE_ROLES,
                            AdminPermissions.EDIT_VENUE_TIMING),
                    ServicePlatformPermissions.ALL,
                    VendorPermissions.ALL
            ).flatMap(List::stream).distinct().toList()
    );

    public static final List<String> SPORTS_ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(CommunityFeedPermissions.VIEW_FEED,
                            CommunityFeedPermissions.CREATE_POST,
                            CommunityFeedPermissions.COMMENT_ON_POST),
                    SportsPermissions.ALL,
                    List.of(MarketplacePermissions.VIEW_MARKETPLACE),
                    List.of(VisitorPermissions.VIEW_VISITORS,
                            VisitorPermissions.CREATE_VISITOR_PASS),
                    List.of(AmenityPermissions.VIEW_AMENITIES,
                            AmenityPermissions.BOOK_AMENITY),
                    List.of(NoticeBoardPermissions.VIEW_NOTICES,
                            NoticeBoardPermissions.CREATE_NOTICE),
                    List.of(HelpdeskPermissions.VIEW_TICKETS,
                            HelpdeskPermissions.CREATE_TICKET),
                    List.of(PollingPermissions.VIEW_POLLS,
                            PollingPermissions.VOTE_POLL),
                    List.of(JobsPermissions.VIEW_JOBS),
                    EventPermissions.ALL,
                    List.of(AdminPermissions.VIEW_ADMIN,
                            AdminPermissions.EDIT_VENUE_TIMING)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> MEMBER_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(CommunityFeedPermissions.VIEW_FEED,
                            CommunityFeedPermissions.CREATE_POST,
                            CommunityFeedPermissions.COMMENT_ON_POST),
                    SportsPermissions.VIEW_ALL,
                    List.of(MarketplacePermissions.VIEW_MARKETPLACE),
                    List.of(VendorPermissions.VIEW_VENDOR_MANAGEMENT,
                            VendorPermissions.BOOK_VENDOR_SERVICE,
                            VendorPermissions.RATE_VENDOR),
                    List.of(VisitorPermissions.VIEW_VISITORS,
                            VisitorPermissions.CREATE_VISITOR_PASS),
                    List.of(AmenityPermissions.VIEW_AMENITIES,
                            AmenityPermissions.BOOK_AMENITY),
                    List.of(NoticeBoardPermissions.VIEW_NOTICES),
                    List.of(HelpdeskPermissions.VIEW_TICKETS,
                            HelpdeskPermissions.CREATE_TICKET),
                    List.of(PollingPermissions.VIEW_POLLS,
                            PollingPermissions.VOTE_POLL),
                    List.of(JobsPermissions.VIEW_JOBS,
                            JobsPermissions.APPLY_JOB),
                    List.of(EventPermissions.VIEW_EVENTS,
                            EventPermissions.REGISTER_EVENT),
                    List.of(ServicePlatformPermissions.VIEW_SERVICE_CATALOG,
                            ServicePlatformPermissions.VIEW_SERVICE_PROVIDERS,
                            ServicePlatformPermissions.CREATE_SERVICE_REQUEST,
                            ServicePlatformPermissions.VIEW_SERVICE_REQUESTS,
                            ServicePlatformPermissions.VIEW_WORK_ORDERS)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> VENDOR_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(CommunityFeedPermissions.VIEW_FEED,
                            CommunityFeedPermissions.CREATE_POST,
                            CommunityFeedPermissions.COMMENT_ON_POST),
                    List.of(SportsPermissions.VIEW_SPORTS_MAIN,
                            SportsPermissions.VIEW_SPORTS_MENU),
                    List.of(MarketplacePermissions.VIEW_MARKETPLACE,
                            MarketplacePermissions.CREATE_LISTING,
                            MarketplacePermissions.DELETE_LISTING),
                    List.of(VisitorPermissions.VIEW_VISITORS),
                    List.of(AmenityPermissions.VIEW_AMENITIES),
                    List.of(NoticeBoardPermissions.VIEW_NOTICES),
                    List.of(HelpdeskPermissions.VIEW_TICKETS,
                            HelpdeskPermissions.CREATE_TICKET),
                    List.of(PollingPermissions.VIEW_POLLS,
                            PollingPermissions.VOTE_POLL),
                    List.of(JobsPermissions.VIEW_JOBS,
                            JobsPermissions.CREATE_JOB),
                    List.of(EventPermissions.VIEW_EVENTS,
                            EventPermissions.REGISTER_EVENT),
                    List.of(VendorPermissions.VIEW_VENDOR_MANAGEMENT,
                            VendorPermissions.CREATE_VENDOR,
                            VendorPermissions.MANAGE_VENDORS,
                            VendorPermissions.MANAGE_VENDOR_PAYMENTS)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> CASHIER_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(CommunityFeedPermissions.VIEW_FEED,
                            CommunityFeedPermissions.COMMENT_ON_POST),
                    List.of(SportsPermissions.VIEW_SPORTS_MAIN,
                            SportsPermissions.VIEW_SPORTS_MENU),
                    List.of(MarketplacePermissions.VIEW_MARKETPLACE),
                    List.of(VisitorPermissions.VIEW_VISITORS),
                    List.of(AmenityPermissions.VIEW_AMENITIES),
                    List.of(NoticeBoardPermissions.VIEW_NOTICES),
                    List.of(HelpdeskPermissions.VIEW_TICKETS),
                    List.of(PollingPermissions.VIEW_POLLS,
                            PollingPermissions.VOTE_POLL),
                    List.of(JobsPermissions.VIEW_JOBS),
                    List.of(EventPermissions.VIEW_EVENTS)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> STAFF_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(CommunityFeedPermissions.VIEW_FEED,
                            CommunityFeedPermissions.COMMENT_ON_POST),
                    List.of(SportsPermissions.VIEW_SPORTS_MAIN,
                            SportsPermissions.VIEW_SPORTS_MENU),
                    List.of(MarketplacePermissions.VIEW_MARKETPLACE),
                    List.of(VisitorPermissions.VIEW_VISITORS,
                            VisitorPermissions.MANAGE_GATE),
                    List.of(AmenityPermissions.VIEW_AMENITIES),
                    List.of(NoticeBoardPermissions.VIEW_NOTICES),
                    List.of(HelpdeskPermissions.VIEW_TICKETS,
                            HelpdeskPermissions.MANAGE_TICKETS),
                    List.of(PollingPermissions.VIEW_POLLS,
                            PollingPermissions.VOTE_POLL),
                    List.of(JobsPermissions.VIEW_JOBS),
                    List.of(EventPermissions.VIEW_EVENTS)
            ).flatMap(List::stream).toList()
    );
}
