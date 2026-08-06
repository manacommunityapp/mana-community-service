package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GroceryStoreResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String logoUrl;
    private String coverImageUrl;
    private String storeType;
    private String status;
    private Boolean deliveryEnabled;
    private BigDecimal minOrder;
    private BigDecimal deliveryFee;
    private BigDecimal rating;
    private Integer totalRatings;
    private Long ownerId;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
