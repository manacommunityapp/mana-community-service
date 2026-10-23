package com.manacommunity.api.groupbuying.dto;

import com.manacommunity.api.groupbuying.model.DemandStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandResponse {
    private String id;
    private String title;
    private String category;
    private String description;
    private Integer interestedResidents;
    private Integer expectedQty;
    private Integer upvotes;
    private Integer targetUpvotes;
    private Boolean hasUpvoted;
    private BigDecimal preferredPriceMin;
    private BigDecimal preferredPriceMax;
    private String preferredBrand;
    private List<VendorOfferDto> vendorOffers;
    private DemandStatus status;
}
