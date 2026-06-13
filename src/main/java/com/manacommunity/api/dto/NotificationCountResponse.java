package com.manacommunity.api.dto;

/**
 * Simple DTO returning the unread notification count for badge display.
 */
public record NotificationCountResponse(long unreadCount) {}
