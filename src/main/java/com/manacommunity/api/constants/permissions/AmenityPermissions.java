package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class AmenityPermissions {

    private AmenityPermissions() {}

    public static final String VIEW_AMENITIES          = "View Amenities";
    public static final String BOOK_AMENITY            = "Book Amenity";
    public static final String MANAGE_AMENITIES        = "Manage Amenities";
    public static final String VIEW_RESOURCE_BOOKING   = "View Resource Booking";
    public static final String MANAGE_RESOURCES        = "Manage Resources";
    public static final String MANAGE_RESOURCE_CATEGORIES = "Manage Resource Categories";
    public static final String MANAGE_BOOKING_RULES    = "Manage Booking Rules";
    public static final String MANAGE_PRICING          = "Manage Pricing";
    public static final String MANAGE_MAINTENANCE      = "Manage Maintenance";
    public static final String APPROVE_BOOKINGS        = "Approve Bookings";
    public static final String VIEW_BOOKING_ANALYTICS  = "View Booking Analytics";
    public static final String MANAGE_COUPONS          = "Manage Coupons";
    public static final String MANAGE_WORKFLOWS        = "Manage Workflows";

    public static final List<String> ALL = List.of(
            VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES,
            VIEW_RESOURCE_BOOKING, MANAGE_RESOURCES, MANAGE_RESOURCE_CATEGORIES,
            MANAGE_BOOKING_RULES, MANAGE_PRICING, MANAGE_MAINTENANCE,
            APPROVE_BOOKINGS, VIEW_BOOKING_ANALYTICS, MANAGE_COUPONS, MANAGE_WORKFLOWS
    );
}
