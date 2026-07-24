package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class HomeChefResponse {
    private Long id;
    private Long userId;
    private String kitchenName;
    private String description;
    private String speciality;
    private String cuisineTypes;
    private String fssaiLicense;
    private String status;
    private String verificationStatus;
    private Integer maxOrdersPerDay;
    private BigDecimal rating;
    private Integer totalRatings;
    private Integer totalOrders;
    private BigDecimal revenueTotal;
    private BigDecimal commissionRate;
    private String profileImageUrl;
    private String coverImageUrl;
    private String availabilityStatus;
    private Integer menuCount;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
