package com.manacommunity.api.groupbuying.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorOfferDto {
    private String id;
    private String demandId;
    private String vendorId;
    private String vendorName;
    private Double vendorRating;
    private Boolean vendorVerified;
    private BigDecimal offeredPrice;
    private Integer minimumQty;
    private Integer maximumQty;
    private String deliveryDate;
    private String validUntil;
    private String terms;
    private Boolean isBestValue;
    private String status;

    // Advanced Vendor Selection Engine & Reliability Metrics
    private Double fulfillmentRate;     // e.g. 99.2%
    private Double onTimeRate;          // e.g. 97.8%
    private Double cancellationRate;    // e.g. 0.8%
    private Double disputeRate;         // e.g. 0.3%
    private Double qualityScore;        // e.g. 4.9 / 5.0
    private Integer compositeScore;     // 0-100 Multi-criteria Best Value match
    private List<String> scoringHighlights;
}
