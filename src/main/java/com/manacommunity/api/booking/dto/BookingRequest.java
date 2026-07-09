package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull
    private Long amenityId;

    @NotNull
    private String bookingDate;

    @NotNull
    private String startTime;

    @NotNull
    private String endTime;

    private String purpose;
}
