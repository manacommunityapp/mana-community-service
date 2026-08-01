package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class RestaurantResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String cuisineTypes;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
    private String fssaiLicense;
    private String gstNumber;
    private String status;
    private BigDecimal rating;
    private Integer totalRatings;
    private BigDecimal commissionRate;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean deliveryEnabled;
    private Boolean takeawayEnabled;
    private Boolean dineInEnabled;
    private BigDecimal minOrderAmount;
    private Integer avgDeliveryTime;
    private Boolean featured;
    private Boolean verified;
    private Long ownerId;
    private Long communityId;
    private List<OperatingHoursResponse> operatingHours;
    private Integer offerCount;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class OperatingHoursResponse {
        private Long id;
        private String dayOfWeek;
        private String openTime;
        private String closeTime;
        private Boolean isClosed;
    }
}
