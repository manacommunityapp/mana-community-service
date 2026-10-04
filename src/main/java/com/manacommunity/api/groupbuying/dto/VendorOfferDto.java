package com.manacommunity.api.groupbuying.dto;

import lombok.*;
import java.math.BigDecimal;

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
}
