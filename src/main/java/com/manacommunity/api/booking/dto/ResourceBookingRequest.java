package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ResourceBookingRequest {

    @NotNull
    private Long resourceId;

    @NotNull
    private String bookingDate;

    @NotNull
    private String startTime;

    @NotNull
    private String endTime;

    private String endDate;
    private String purpose;
    private Integer numberOfGuests;
    private List<Long> equipmentIds;
    private String couponCode;
}
