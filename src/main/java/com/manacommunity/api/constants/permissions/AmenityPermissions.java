package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class AmenityPermissions {

    private AmenityPermissions() {}

    public static final String VIEW_AMENITIES   = "View Amenities";
    public static final String BOOK_AMENITY     = "Book Amenity";
    public static final String MANAGE_AMENITIES = "Manage Amenities";

    public static final List<String> ALL = List.of(
            VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES
    );
}
