package com.manacommunity.api.fresh.processing.dto;

import com.manacommunity.api.fresh.processing.model.BatchStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class BatchDto {
    private Long id;
    private String batchNumber;
    private Long vendorId;
    private Long recipeId;
    private String recipeName;
    private Long vendorProductId;
    private String productName;
    private LocalDate productionDate;
    private BigDecimal rawMaterialInputQty;
    private BigDecimal actualOutputQty;
    private BigDecimal actualYieldPercent;
    private BigDecimal wastageQty;
    private String wastageReason;
    private BatchStatus status;
    private BigDecimal storageTempCelsius;
    private Instant expiryTimestamp;
    private String notes;
    private Instant createdAt;
    private BatchReleaseDto release;
    private List<QualityCheckDto> qualityChecks;
}