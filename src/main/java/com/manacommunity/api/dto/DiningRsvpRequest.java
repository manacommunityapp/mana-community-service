package com.manacommunity.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiningRsvpRequest {
    @NotNull
    private Long diningEventId;
    @Min(1)
    private int guestCount = 1;
    private String specialRequests;
}
