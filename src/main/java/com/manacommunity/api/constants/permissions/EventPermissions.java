package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class EventPermissions {

    private EventPermissions() {}

    // Core
    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";

    // Dashboard
    public static final String VIEW_EVENT_DASHBOARD   = "View Event Dashboard";
    public static final String MANAGE_EVENT_DASHBOARD  = "Manage Event Dashboard";

    // Events & Schedule
    public static final String VIEW_EVENT_SCHEDULE        = "View Event Schedule";
    public static final String CREATE_EDIT_EVENT_SCHEDULE  = "Create/Edit Event Schedule";
    public static final String DELETE_EVENT_SCHEDULE       = "Delete Event Schedule";

    // Registration
    public static final String VIEW_EVENT_REGISTRATION   = "View Event Registration";
    public static final String MANAGE_EVENT_REGISTRATION  = "Manage Event Registration";
    public static final String EXPORT_EVENT_REGISTRATION  = "Export Event Registration";

    // People / Volunteers
    public static final String VIEW_EVENT_PEOPLE   = "View Event People";
    public static final String MANAGE_EVENT_PEOPLE  = "Manage Event People";

    // Fundraising / Finance
    public static final String VIEW_EVENT_FUNDRAISING   = "View Event Fundraising";
    public static final String MANAGE_EVENT_FUNDRAISING  = "Manage Event Fundraising";

    // Operations
    public static final String VIEW_EVENT_OPERATIONS   = "View Event Operations";
    public static final String MANAGE_EVENT_OPERATIONS  = "Manage Event Operations";

    // Media & Reports
    public static final String VIEW_EVENT_MEDIA   = "View Event Media";
    public static final String MANAGE_EVENT_MEDIA  = "Manage Event Media";
    public static final String VIEW_EVENT_GALLERY  = "View Event Gallery";
    public static final String VIEW_EVENT_REPORTS  = "View Event Reports";

    // Notifications
    public static final String SEND_EVENT_NOTIFICATIONS   = "Send Event Notifications";
    public static final String MANAGE_EVENT_NOTIFICATIONS  = "Manage Event Notifications";

    // Admin Dashboard
    public static final String VIEW_ADMIN_DASHBOARD       = "View Event Admin Dashboard";

    public static final String VIEW_USER_EVENT_DASHBOARD       = "View Event User Dashboard";

    public static final String MANAGE_USER_EVENT_DASHBOARD  = "Manage User Event Dashboard";

    // Forms — Food & Cultural, Pooja, Donation, Auctions
    public static final String VIEW_EVENT_FORMS            = "View Event Forms";
    public static final String MANAGE_EVENT_FORMS          = "Manage Event Forms";
    public static final String DELETE_EVENT_FORMS          = "Delete Event Forms";

    public static final List<String> VIEW_ALL = List.of(
            VIEW_EVENTS, REGISTER_EVENT,
            VIEW_EVENT_DASHBOARD, VIEW_EVENT_SCHEDULE,
            VIEW_EVENT_REGISTRATION, VIEW_EVENT_PEOPLE,
            VIEW_EVENT_FUNDRAISING, VIEW_EVENT_OPERATIONS,
            VIEW_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS,
            VIEW_EVENT_FORMS
    );

    public static final List<String> ALL = List.of(
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT,
            VIEW_EVENT_DASHBOARD, MANAGE_EVENT_DASHBOARD,
            VIEW_EVENT_SCHEDULE, CREATE_EDIT_EVENT_SCHEDULE, DELETE_EVENT_SCHEDULE,
            VIEW_EVENT_REGISTRATION, MANAGE_EVENT_REGISTRATION, EXPORT_EVENT_REGISTRATION,
            VIEW_EVENT_PEOPLE, MANAGE_EVENT_PEOPLE,
            VIEW_EVENT_FUNDRAISING, MANAGE_EVENT_FUNDRAISING,
            VIEW_EVENT_OPERATIONS, MANAGE_EVENT_OPERATIONS,
            VIEW_EVENT_MEDIA, MANAGE_EVENT_MEDIA, VIEW_EVENT_GALLERY, VIEW_EVENT_REPORTS,
            SEND_EVENT_NOTIFICATIONS, MANAGE_EVENT_NOTIFICATIONS,
            VIEW_ADMIN_DASHBOARD, VIEW_EVENT_FORMS, MANAGE_EVENT_FORMS, DELETE_EVENT_FORMS,
            VIEW_USER_EVENT_DASHBOARD,MANAGE_USER_EVENT_DASHBOARD
    );
}
