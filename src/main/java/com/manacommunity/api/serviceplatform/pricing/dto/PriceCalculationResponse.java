package com.manacommunity.api.serviceplatform.pricing.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCalculationResponse {
    private BigDecimal basePrice;
    private BigDecimal surcharge;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String breakdown;
}
