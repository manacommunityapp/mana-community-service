package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CateringQuotationRequest {
    @NotNull
    private Long requestId;
    private String menu;
    @NotNull
    private BigDecimal pricePerPlate;
    @NotNull
    private BigDecimal totalAmount;
    private LocalDate validUntil;
    private String notes;
}
