package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for POST /api/events/pooja-registrations/{id}/reschedule */
@Data
public class PoojaRescheduleRequest {

    @NotNull(message = "newScheduleId is required")
    private Long newScheduleId;

    /** Client-supplied UUID used to make the underlying reserve() call idempotent. */
    private String idempotencyKey;
}
