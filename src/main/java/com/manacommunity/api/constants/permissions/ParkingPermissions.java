package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class ParkingPermissions {

    private ParkingPermissions() {}

    public static final String VIEW_PARKING   = "View Parking";
    public static final String MANAGE_PARKING = "Manage Parking";

    public static final List<String> ALL = List.of(VIEW_PARKING, MANAGE_PARKING);
}
