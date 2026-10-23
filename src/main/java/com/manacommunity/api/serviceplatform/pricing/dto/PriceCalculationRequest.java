package com.manacommunity.api.serviceplatform.pricing.dto;

import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCalculationRequest {
    private Long categoryId;
    private Long offeringId;
    private ServiceUrgency urgency;
    private String couponCode;
    private BigDecimal baseAmount;
}
