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
    public static final String ROLE_USER           = "USER";   // Default role for all new registrations
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
    public static final String MANAGE_MARKETPLACE = "Manage Marketplace";

    // ──── VISITOR / GATE PASS ──────────────────────────────────────
    public static final String VIEW_VISITORS      = "View Visitors";
    public static final String CREATE_VISITOR_PASS = "Create Visitor Pass";
    public static final String MANAGE_GATE        = "Manage Gate";

    // ──── AMENITY BOOKING ────────────────────────────────────────────
    public static final String VIEW_AMENITIES    = "View Amenities";
    public static final String BOOK_AMENITY      = "Book Amenity";
    public static final String MANAGE_AMENITIES  = "Manage Amenities";

    // ──── NOTICE BOARD ───────────────────────────────────────────────
    public static final String VIEW_NOTICES   = "View Notices";
    public static final String CREATE_NOTICE  = "Create Notice";
    public static final String DELETE_NOTICE  = "Delete Notice";

    // ──── HELPDESK / COMPLAINTS ────────────────────────────────────
    public static final String VIEW_TICKETS    = "View Tickets";
    public static final String CREATE_TICKET   = "Create Ticket";
    public static final String MANAGE_TICKETS  = "Manage Tickets";

    // ──── POLLING / VOTING ─────────────────────────────────────────
    public static final String VIEW_POLLS   = "View Polls";
    public static final String CREATE_POLL  = "Create Poll";
    public static final String VOTE_POLL    = "Vote Poll";

    // ──── JOBS & REFERRALS ──────────────────────────────────────────
    public static final String VIEW_JOBS  = "View Jobs";
    public static final String CREATE_JOB = "Create Job";
    public static final String APPLY_JOB  = "Apply Job";

    // ──── EVENTS — GRANULAR PERMISSIONS ────
    // Core
    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";
    // Dashboard
    public static final String VIEW_EVENT_DASHBOARD        = "View Event Dashboard";
    public static final String MANAGE_EVENT_DASHBOARD      = "Manage Event Dashboard";
    // Events & Schedule
    public static final String VIEW_EVENT_SCHEDULE         = "View Event Schedule";
    public static final String CREATE_EDIT_EVENT_SCHEDULE  = "Create/Edit Event Schedule";
    public static final String DELETE_EVENT_SCHEDULE       = "Delete Event Schedule";
    // Registration
    public static final String VIEW_EVENT_REGISTRATION     = "View Event Registration";
    public static final String MANAGE_EVENT_REGISTRATION   = "Manage Event Registration";
    public static final String EXPORT_EVENT_REGISTRATION   = "Export Event Registration";
    // People / Volunteers
    public static final String VIEW_EVENT_PEOPLE           = "View Event People";
    public static final String MANAGE_EVENT_PEOPLE         = "Manage Event People";
    // Fundraising / Finance
    public static final String VIEW_EVENT_FUNDRAISING      = "View Event Fundraising";
    public static final String MANAGE_EVENT_FUNDRAISING    = "Manage Event Fundraising";
    // Operations
    public static final String VIEW_EVENT_OPERATIONS       = "View Event Operations";
    public static final String MANAGE_EVENT_OPERATIONS     = "Manage Event Operations";
    // Media & Reports
    public static final String VIEW_EVENT_MEDIA            = "View Event Media";
    public static final String MANAGE_EVENT_MEDIA          = "Manage Event Media";
    public static final String VIEW_EVENT_GALLERY          = "View Event Gallery";
    public static final String VIEW_EVENT_REPORTS          = "View Event Reports";
    // Notifications
    public static final String SEND_EVENT_NOTIFICATIONS    = "Send Event Notifications";
    public static final String MANAGE_EVENT_NOTIFICATIONS  = "Manage Event Notifications";


    // ──── SERVICE PLATFORM ────────────────────────────────────────────
    public static final String VIEW_SERVICE_CATALOG     = "View Service Catalog";
    public static final String MANAGE_SERVICE_CATALOG   = "Manage Service Catalog";
    public static final String VIEW_SERVICE_PROVIDERS   = "View Service Providers";
    public static final String MANAGE_SERVICE_PROVIDERS = "Manage Service Providers";
    public static final String CREATE_SERVICE_REQUEST   = "Create Service Request";
    public static final String VIEW_SERVICE_REQUESTS    = "View Service Requests";
    public static final String MANAGE_SERVICE_REQUESTS  = "Manage Service Requests";
    public static final String VIEW_WORK_ORDERS         = "View Work Orders";
    public static final String MANAGE_WORK_ORDERS       = "Manage Work Orders";

    // ──── VENDOR MANAGEMENT SYSTEM ─────────────────────────────────
    public static final String VIEW_VENDOR_MANAGEMENT  = "View Vendor Management";
    public static final String CREATE_VENDOR           = "Create Vendor";
    public static final String MANAGE_VENDORS          = "Manage Vendors";
    public static final String BOOK_VENDOR_SERVICE     = "Book Vendor Service";
    public static final String MANAGE_PROCUREMENT      = "Manage Procurement";
    public static final String MANAGE_CONTRACTS        = "Manage Contracts";
    public static final String MANAGE_VENDOR_PAYMENTS  = "Manage Vendor Payments";
    public static final String RATE_VENDOR             = "Rate Vendor";
    public static final String VIEW_VENDOR_ANALYTICS   = "View Vendor Analytics";


    // ──── FOOD & LIFESTYLE OS ─────────────────────────────────────
    public static final String VIEW_FOOD_PROFILE       = "View Food Profile";
    public static final String MANAGE_FOOD_PROFILE     = "Manage Food Profile";
    public static final String VIEW_FOOD_RESTAURANTS   = "View Food Restaurants";
    public static final String MANAGE_FOOD_RESTAURANTS = "Manage Food Restaurants";
    public static final String VIEW_FOOD_MENU          = "View Food Menu";
    public static final String MANAGE_FOOD_MENU        = "Manage Food Menu";
    public static final String VIEW_FOOD_HOME_CHEFS    = "View Food Home Chefs";
    public static final String MANAGE_FOOD_HOME_CHEFS  = "Manage Food Home Chefs";
    public static final String VIEW_FOOD_ORDERS        = "View Food Orders";
    public static final String MANAGE_FOOD_ORDERS      = "Manage Food Orders";
    public static final String VIEW_FOOD_SUBSCRIPTIONS = "View Food Subscriptions";
    public static final String MANAGE_FOOD_SUBSCRIPTIONS = "Manage Food Subscriptions";
    public static final String VIEW_FOOD_DINING        = "View Food Dining";
    public static final String MANAGE_FOOD_DINING      = "Manage Food Dining";
    public static final String VIEW_FOOD_GROCERY       = "View Food Grocery";
    public static final String MANAGE_FOOD_GROCERY     = "Manage Food Grocery";
    public static final String VIEW_FOOD_RECIPES       = "View Food Recipes";
    public static final String MANAGE_FOOD_RECIPES     = "Manage Food Recipes";
    public static final String VIEW_FOOD_NUTRITION     = "View Food Nutrition";
    public static final String MANAGE_FOOD_NUTRITION   = "Manage Food Nutrition";
    public static final String VIEW_FOOD_DELIVERY      = "View Food Delivery";
    public static final String MANAGE_FOOD_DELIVERY    = "Manage Food Delivery";
    public static final String VIEW_FOOD_COMMUNITY_KITCHEN  = "View Food Community Kitchen";
    public static final String MANAGE_FOOD_COMMUNITY_KITCHEN = "Manage Food Community Kitchen";
    public static final String VIEW_FOOD_CATERING      = "View Food Catering";
    public static final String MANAGE_FOOD_CATERING    = "Manage Food Catering";
    public static final String VIEW_FOOD_CORPORATE     = "View Food Corporate";
    public static final String MANAGE_FOOD_CORPORATE   = "Manage Food Corporate";
    public static final String VIEW_FOOD_EVENTS        = "View Food Events";
    public static final String MANAGE_FOOD_EVENTS      = "Manage Food Events";
    public static final String VIEW_FOOD_PANTRY        = "View Food Pantry";
    public static final String MANAGE_FOOD_PANTRY      = "Manage Food Pantry";
    public static final String VIEW_FOOD_LOYALTY       = "View Food Loyalty";
    public static final String MANAGE_FOOD_LOYALTY     = "Manage Food Loyalty";
    public static final String VIEW_FOOD_ANALYTICS     = "View Food Analytics";
    public static final String VIEW_FOOD_PAYMENTS      = "View Food Payments";
    public static final String MANAGE_FOOD_PAYMENTS    = "Manage Food Payments";
    public static final String VIEW_FOOD_CLOUD_KITCHENS  = "View Food Cloud Kitchens";
    public static final String MANAGE_FOOD_CLOUD_KITCHENS = "Manage Food Cloud Kitchens";

    // ──── ADMIN DASHBOARD ───────────────────────────────────────────
    public static final String VIEW_ADMIN         = "View Admin";
    public static final String VERIFY_KYC         = "Verify KYC";
    public static final String BULK_UPLOAD        = "Bulk Upload";
    public static final String MANAGE_COMMUNITIES = "Manage Communities";
    public static final String MANAGE_ROLES       = "Manage Roles";
    public static final String EDIT_VENUE_TIMING  = "Edit Venue Timing";

    // ──── EVENT PERMISSION GROUPS ───────────────────────────────────
    /** All 11 View event permissions */
    public static final List<String> ALL_EVENT_VIEW_PERMISSIONS = List.of(
            VIEW_EVENTS, REGISTER_EVENT,
            VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE,
            VIEW_EVENT_REGISTRATION, VIEW_EVENT_PEOPLE,
            VIEW_EVENT_FUNDRAISING, VIEW_EVENT_OPERATIONS,
            VIEW_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS
    );

    /** All 23 event permissions */
    public static final List<String> ALL_EVENT_PERMISSIONS = List.of(
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT,
            VIEW_EVENT_DASHBOARD, MANAGE_EVENT_DASHBOARD,
            VIEW_EVENT_SCHEDULE, CREATE_EDIT_EVENT_SCHEDULE, DELETE_EVENT_SCHEDULE,
            VIEW_EVENT_REGISTRATION, MANAGE_EVENT_REGISTRATION, EXPORT_EVENT_REGISTRATION,
            VIEW_EVENT_PEOPLE, MANAGE_EVENT_PEOPLE,
            VIEW_EVENT_FUNDRAISING, MANAGE_EVENT_FUNDRAISING,
            VIEW_EVENT_OPERATIONS, MANAGE_EVENT_OPERATIONS,
            VIEW_EVENT_MEDIA, MANAGE_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS,
            SEND_EVENT_NOTIFICATIONS, MANAGE_EVENT_NOTIFICATIONS
    );

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
                    ALL_EVENT_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING, MANAGE_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES),
                    List.of(VIEW_NOTICES, CREATE_NOTICE, DELETE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET, MANAGE_TICKETS),
                    List.of(VIEW_POLLS, CREATE_POLL, VOTE_POLL),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_COMMUNITIES, MANAGE_ROLES, EDIT_VENUE_TIMING),

                    List.of(VIEW_SERVICE_CATALOG, MANAGE_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
                            MANAGE_SERVICE_PROVIDERS, CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS,
                            MANAGE_SERVICE_REQUESTS, VIEW_WORK_ORDERS, MANAGE_WORK_ORDERS),

                    List.of(VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, BOOK_VENDOR_SERVICE,
                            MANAGE_PROCUREMENT, MANAGE_CONTRACTS, MANAGE_VENDOR_PAYMENTS,
                            RATE_VENDOR, VIEW_VENDOR_ANALYTICS)

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
                    List.of(VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    ALL_EVENT_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING, MANAGE_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES),
                    List.of(VIEW_NOTICES, CREATE_NOTICE, DELETE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET, MANAGE_TICKETS),
                    List.of(VIEW_POLLS, CREATE_POLL, VOTE_POLL),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_ROLES, EDIT_VENUE_TIMING),

                    List.of(VIEW_SERVICE_CATALOG, MANAGE_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
                            MANAGE_SERVICE_PROVIDERS, CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS,
                            MANAGE_SERVICE_REQUESTS, VIEW_WORK_ORDERS, MANAGE_WORK_ORDERS),

                    List.of(VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, BOOK_VENDOR_SERVICE,
                            MANAGE_PROCUREMENT, MANAGE_CONTRACTS, MANAGE_VENDOR_PAYMENTS,
                            RATE_VENDOR, VIEW_VENDOR_ANALYTICS)
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
                    List.of(VIEW_FEED, CREATE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY),
                    List.of(VIEW_NOTICES, CREATE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET),
                    List.of(VIEW_POLLS, VOTE_POLL),
                    List.of(VIEW_JOBS),
                    List.of(VIEW_EVENTS, REGISTER_EVENT, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE, VIEW_EVENT_REGISTRATION, VIEW_EVENT_PEOPLE, VIEW_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS, SEND_EVENT_NOTIFICATIONS),
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
            VIEW_VENDOR_MANAGEMENT, BOOK_VENDOR_SERVICE, RATE_VENDOR,
            VIEW_VISITORS, CREATE_VISITOR_PASS,
            VIEW_AMENITIES, BOOK_AMENITY,
            VIEW_NOTICES,
            VIEW_TICKETS, CREATE_TICKET,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS, APPLY_JOB,
            VIEW_EVENTS, REGISTER_EVENT, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE, VIEW_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS,
            VIEW_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
            CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS, VIEW_WORK_ORDERS
    );

    public static final List<String> VENDOR_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
            VIEW_VISITORS,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS, CREATE_TICKET,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS, CREATE_JOB,
            VIEW_EVENTS, REGISTER_EVENT, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE,
            VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, MANAGE_VENDOR_PAYMENTS
    );

    public static final List<String> CASHIER_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS,
            VIEW_EVENTS, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE, VIEW_EVENT_REGISTRATION
    );

    public static final List<String> STAFF_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS, MANAGE_GATE,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS, MANAGE_TICKETS,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS,
            VIEW_EVENTS, REGISTER_EVENT, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE, VIEW_EVENT_REGISTRATION, VIEW_EVENT_PEOPLE, VIEW_EVENT_OPERATIONS, VIEW_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS
    );

    /**
     * Default permissions for a newly registered USER (minimal view-only access).
     * Admins can upgrade to MEMBER or higher at any time.
     */
    public static final List<String> USER_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_EVENTS, REGISTER_EVENT, VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE, VIEW_EVENT_GALLERY,
            VIEW_MARKETPLACE,
            VIEW_NOTICES,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS,
            VIEW_AMENITIES
    );
}
