package com.manacommunity.api.mobility.model;

public enum MobilityTripStatus {
    REQUESTED, SEARCHING, ASSIGNED, DRIVER_ARRIVING, ARRIVED,
    PICKED_UP, IN_PROGRESS, COMPLETED,
    CANCELLED, NO_DRIVER_FOUND, EXPIRED
}
