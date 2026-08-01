package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CateringQuotationResponse {
    private Long id;
    private Long requestId;
    private Long catererId;
    private String catererName;
    private String catererLogoUrl;
    private String menu;
    private BigDecimal pricePerPlate;
    private BigDecimal totalAmount;
    private LocalDate validUntil;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
