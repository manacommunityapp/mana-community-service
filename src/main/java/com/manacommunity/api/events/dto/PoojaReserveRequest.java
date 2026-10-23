package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/events/pooja-schedules/{scheduleId}/reserve */
@Data
public class PoojaReserveRequest {

    @NotBlank(message = "idempotencyKey is required — supply a UUID to make retries safe")
    private String idempotencyKey;

    @Min(value = 1, message = "familyCount must be at least 1")
    private int familyCount = 1;

    @Min(value = 1, message = "devoteeCount must be at least 1")
    private int devoteeCount = 1;

    public void setFamilyCount(int familyCount) { this.familyCount = Math.max(1, familyCount); }
    public void setDevoteeCount(int devoteeCount) { this.devoteeCount = Math.max(1, devoteeCount); }
}
