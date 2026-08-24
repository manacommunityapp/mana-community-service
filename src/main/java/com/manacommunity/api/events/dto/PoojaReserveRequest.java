package com.manacommunity.api.events.dto;

import lombok.Data;

/** Request body for POST /api/events/pooja-schedules/{scheduleId}/reserve */
@Data
public class PoojaReserveRequest {

    /** Idempotency key supplied by the client (UUID recommended). */
    private String idempotencyKey;

    /** Number of family units to reserve (default 1). */
    private int familyCount = 1;

    /** Number of devotees to reserve (default 1). */
    private int devoteeCount = 1;
}
