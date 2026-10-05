package com.manacommunity.api.groupbuying.dto;

import com.manacommunity.api.groupbuying.model.FulfillmentType;
import com.manacommunity.api.groupbuying.model.PaymentType;
import com.manacommunity.api.groupbuying.model.PricingModel;
import com.manacommunity.api.groupbuying.model.PricingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVendorDealRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String category;
    private String subCategory;
    private String description;
    private String imageUrl;
    @NotNull
    private BigDecimal mrp;
    @NotNull
    private BigDecimal standardPrice;
    @NotNull
    private BigDecimal currentPrice;
    @NotNull
    private Integer targetQty;
    private PricingModel pricingModel;
    private String pickupPoint;
    private LocalDateTime dealEndsAt;
}
