package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long categoryId;

    private String description;
    private Integer capacity;
    private String location;
    private String building;
    private String floor;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String openTime;
    private String closeTime;
    private Integer bookingDurationMinutes;
    private Integer minimumDurationMinutes;
    private Integer maximumDurationMinutes;
    private Integer bufferTimeMinutes;
    private Integer cleaningTimeMinutes;
    private Integer advanceBookingDays;
    private Integer maxBookingsPerUser;
    private Integer maxActiveBookings;
    private Integer cancellationHours;
    private Boolean autoCancel;
    private Boolean approvalRequired;
    private Boolean depositRequired;
    private Boolean paymentRequired;
    private Boolean allowWaitlist;
    private Boolean allowGuest;
    private Boolean qrCheckIn;
    private Boolean recurringBookingAllowed;
    private Integer maxCapacity;
    private String bookingType;
    private String status;
}
