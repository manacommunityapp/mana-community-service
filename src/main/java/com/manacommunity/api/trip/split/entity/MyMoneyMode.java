package com.manacommunity.api.trip.split.entity;

/**
 * How a user wants a trip reflected in My Money. One mode at a time, because recording both a share
 * and the settlement payments for the same money would double-count it.
 */
public enum MyMoneyMode {
    /** Nothing is written to My Money. Default. */
    OFF,
    /** My share of each expense is recorded as a personal expense. Recommended. */
    SHARE,
    /** Settlement payments I send are recorded as expenses and ones I receive as income. */
    SETTLEMENTS
}
