package com.manacommunity.api.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupDealRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String category;

    private String subCategory;
    private String imageUrl;
    private BigDecimal mrp;
    private String vendorName;
    private BigDecimal vendorRating;
    private Boolean vendorVerified;

    @Positive
    @NotNull
    private Integer targetParticipants;

    @NotNull
    private LocalDateTime endDate;

    private LocalDateTime deliveryDate;

    private Boolean isFestival;
    private String festivalName;
    private List<String> tags;
    private List<String> pickupPoints;

    @NotEmpty
    private List<TierRequest> tiers;

    @Data
    public static class TierRequest {
        @Positive @NotNull private Integer minQty;
        private Integer maxQty;
        @NotNull private BigDecimal pricePerUnit;
        private String label;
    }
}
