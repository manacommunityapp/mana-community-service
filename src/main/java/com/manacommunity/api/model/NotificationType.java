package com.manacommunity.api.model;

/**
 * Types of user-facing notifications stored in the notification table.
 */
public enum NotificationType {
    SCHEDULE_PUBLISHED,
    SCHEDULE_UPDATED,
    MATCH_REMINDER,
    MATCH_RESULT_POSTED,
    REGISTRATION_OPEN,
    REGISTRATION_CONFIRMED,
    REGISTRATION_REJECTED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    AUCTION_STARTED,
    AUCTION_COMPLETED,
    TEAM_ASSIGNED,
    GENERAL
}
