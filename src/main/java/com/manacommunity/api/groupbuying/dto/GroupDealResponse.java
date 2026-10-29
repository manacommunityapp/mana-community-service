package com.manacommunity.api.groupbuying.dto;

import com.manacommunity.api.groupbuying.model.DealStatus;
import com.manacommunity.api.groupbuying.model.FulfillmentType;
import com.manacommunity.api.groupbuying.model.PaymentType;
import com.manacommunity.api.groupbuying.model.PricingModel;
import com.manacommunity.api.groupbuying.model.PricingType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDealResponse {
    private String id;
    private String title;
    private String category;
    private String subCategory;
    private String description;
    private String imageUrl;
    private String vendor;
    private String vendorName;
    private String vendorId;
    private Double vendorRating;
    private Boolean vendorVerified;

    private PricingModel pricingModel;
    private PricingType pricingType;
    private BigDecimal mrp;
    private BigDecimal standardPrice;
    private BigDecimal currentPrice;
    private BigDecimal currentTierPrice;
    private BigDecimal nextTierPrice;
    private Integer nextTierUnitsNeeded;

    private List<PriceTierDto> priceTiers;
    private Integer committedQty;
    private Integer targetQty;
    private Integer currentParticipants;
    private Integer targetParticipants;
    private Integer inventoryRemaining;
    private String moqLabel;

    private DealStatus dealStatus;
    private Integer daysLeft;
    private Long hoursRemaining;
    private LocalDateTime dealEndsAt;
    private LocalDateTime priceLockedAt;
    private LocalDateTime createdAt;

    private String pickupPoint;
    private LocalDateTime pickupDate;
    private List<String> pickupSlots;
    private FulfillmentType fulfillmentType;
    private PaymentType paymentType;

    private Boolean isTrending;
    private Boolean isAlmostUnlocked;
    private Boolean isFestivalDeal;
    private Boolean isEndingSoon;
}
