package com.manacommunity.api.model;

/**
 * Polymorphic reference type for notifications — indicates which entity
 * the notification refers to.
 */
public enum ReferenceType {
    TOURNAMENT_CONFIG,
    SPORTS_EVENT,
    TOURNAMENT_MATCH,
    AUCTION_CONFIG,
    AUCTION_TEAM,
    COMMUNITY
}
