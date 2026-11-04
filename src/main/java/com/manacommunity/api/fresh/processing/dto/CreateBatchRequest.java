package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class CreateBatchRequest {
    private Long vendorId;
    private Long recipeId;
    private Long vendorProductId;
    private LocalDate productionDate;
    private BigDecimal rawMaterialInputQty;
    private BigDecimal storageTempCelsius;
    private Instant expiryTimestamp;
    private String notes;
}