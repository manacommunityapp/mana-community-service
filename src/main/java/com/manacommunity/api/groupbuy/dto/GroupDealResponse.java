package com.manacommunity.api.groupbuy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupDealResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String imageUrl;
    private BigDecimal mrp;
    private String vendor;
    private Long vendorId;
    private BigDecimal vendorRating;
    private Boolean vendorVerified;
    private Integer targetParticipants;
    private Integer currentParticipants;
    private String endDate;
    private String deliveryDate;
    private String status;
    private Boolean isFestival;
    private String festivalName;
    private List<String> tags;
    private List<String> pickupPoints;
    private List<TierResponse> tiers;
    private Boolean hasJoined;
    private BigDecimal currentTierPrice;
    private BigDecimal nextTierPrice;
    private Integer nextTierUnitsNeeded;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class TierResponse {
        private Integer minQty;
        private Integer maxQty;
        private BigDecimal pricePerUnit;
        private String label;
    }
}
