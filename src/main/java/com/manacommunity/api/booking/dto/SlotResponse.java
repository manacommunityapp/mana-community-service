package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlotResponse {

    private String startTime;
    private String endTime;
    private boolean available;
    private Long bookingId;
    private String bookedByName;
}
