package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CatererResponse {
    private Long id;
    private String name;
    private String description;
    private String cuisineTypes;
    private Integer minOrderCount;
    private Integer maxOrderCount;
    private BigDecimal pricePerPlateFrom;
    private BigDecimal pricePerPlateTo;
    private String fssaiLicense;
    private String logoUrl;
    private String status;
    private BigDecimal rating;
    private Integer totalRatings;
    private List<PackageResponse> packages;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class PackageResponse {
        private Long id;
        private String name;
        private String description;
        private BigDecimal pricePerPlate;
        private String menuItems;
        private Boolean active;
    }
}
