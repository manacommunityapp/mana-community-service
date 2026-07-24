package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOfferingRequest {
    @NotNull
    private Long categoryId;
    @NotBlank @Size(max = 200)
    private String title;
    private String description;
    @NotNull
    private BigDecimal basePrice;
    @NotBlank
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private BigDecimal minOrderValue;
    private String customFieldValues;
    private String tags;
}
