package com.manacommunity.api.dto;

import java.util.List;

/**
 * Request body for marking specific notifications as read.
 */
public record MarkReadRequest(List<Long> notificationIds) {}
