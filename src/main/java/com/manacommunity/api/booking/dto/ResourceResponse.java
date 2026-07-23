package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResourceResponse {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
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
    private Boolean deleted;
    private String primaryImageUrl;
    private Long totalBookings;
    private Long activeBookings;
    private Long communityId;
    private String createdAt;
    private String updatedAt;
}
