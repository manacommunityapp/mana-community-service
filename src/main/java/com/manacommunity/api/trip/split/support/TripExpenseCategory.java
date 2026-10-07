package com.manacommunity.api.trip.split.support;

import com.manacommunity.api.exception.InvalidInputException;

public enum TripExpenseCategory {
    ACCOMMODATION, TRANSPORT, FUEL, FOOD, ACTIVITIES, TICKETS, SHOPPING, PARKING, TOLL, MISCELLANEOUS;

    /** Null or blank defaults to MISCELLANEOUS; anything unrecognised is rejected. */
    public static TripExpenseCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MISCELLANEOUS;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("Unknown expense category: " + code);
        }
    }
}
