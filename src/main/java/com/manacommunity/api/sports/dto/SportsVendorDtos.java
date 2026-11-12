package com.manacommunity.api.sports.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SportsVendorDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilityResponse {
        private Long id;
        private Long vendorId;
        private String name;
        private String address;
        private String area;
        private String city;
        private String phone;
        private String email;
        private String openingTime;
        private String closingTime;
        private Boolean isFederated;
        private BigDecimal rating;
        private Integer reviewCount;
        private Integer courtsCount;
        private List<String> sports;
        private List<String> amenities;
        private List<String> images;
        private List<String> partnerCommunities;
        private List<CourtResponse> courts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtResponse {
        private Long id;
        private Long facilityId;
        private String name;
        private String sportType;
        private String surfaceType;
        private Boolean isIndoor;
        private Boolean hasLighting;
        private BigDecimal baseHourlyPrice;
        private BigDecimal residentDiscountPercent;
        private String rules;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotResponse {
        private Long id;
        private Long courtId;
        private LocalDate slotDate;
        private String startTime;
        private String endTime;
        private String status;
        private BigDecimal price;
        private BigDecimal residentPrice;
        private LocalDateTime heldUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBookingRequest {
        private Long venueId;
        private Long courtId;
        private Long slotId;
        private LocalDate bookingDate;
        private String startTime;
        private String endTime;
        private String residentName;
        private String residentPhone;
        private String residentFlat;
        private String communityName;
        private BigDecimal totalAmount;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingResponse {
        private Long id;
        private String bookingNumber;
        private Long venueId;
        private Long courtId;
        private Long slotId;
        private Long residentId;
        private String residentName;
        private String residentPhone;
        private String residentFlat;
        private String communityName;
        private LocalDate bookingDate;
        private String startTime;
        private String endTime;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private String bookingStatus;
        private String qrCodeToken;
        private LocalDateTime checkedInAt;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoachResponse {
        private Long id;
        private String name;
        private String bio;
        private String phone;
        private String email;
        private String avatarUrl;
        private Integer experienceYears;
        private BigDecimal hourlyRate;
        private BigDecimal rating;
        private Integer reviewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResponse {
        private Long id;
        private Long coachId;
        private String coachName;
        private Long venueId;
        private String venueName;
        private String name;
        private String sport;
        private String skillLevel;
        private String ageGroup;
        private String startTime;
        private String endTime;
        private Integer maxCapacity;
        private Integer currentEnrolled;
        private BigDecimal monthlyFee;
        private LocalDate startDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfficialResponse {
        private Long id;
        private String name;
        private String sport;
        private String officialRole;
        private String badgeLevel;
        private BigDecimal dailyRate;
        private BigDecimal hourlyRate;
        private Integer matchesOfficiated;
        private BigDecimal rating;
        private String phone;
        private String avatarUrl;
        private Boolean isAvailable;
    }
}
