package com.manacommunity.api.security;

/**
 * Catalogue of auditable business events. Stored as {@code name()} in the
 * {@code audit_log.action} string column (not as a DB enum) so the catalogue can
 * evolve without breaking historical rows.
 */
public enum AuditAction {

    // ── User Management ──
    USER_CREATED, USER_UPDATED, USER_DELETED, ROLE_CHANGED, PERMISSION_CHANGED,

    // ── Sports ──
    PLAYER_REGISTERED, MATCH_CREATED, MATCH_UPDATED, WINNER_DECLARED, TOURNAMENT_COMPLETED,

    // ── Auction ──
    AUCTION_STARTED, AUCTION_ENDED, BID_PLACED, PLAYER_SOLD, TEAM_CREATED,

    // ── Marketplace ──
    PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED,
    ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED,
    DONATION_CREATED, DONATION_CLAIMED, DONATION_DELETED,
    LOST_FOUND_CREATED, LOST_FOUND_RESOLVED, LOST_FOUND_CLOSED,

    // ── Admin / Config ──
    CONFIG_UPDATED
}
