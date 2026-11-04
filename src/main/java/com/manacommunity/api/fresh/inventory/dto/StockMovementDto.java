package com.manacommunity.api.fresh.inventory.dto;

import com.manacommunity.api.fresh.inventory.model.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {
    private Long id;
    private Long vendorProductId;
    private String productName;
    private Long batchId;
    private String batchNumber;
    private Long fulfilmentLocationId;
    private String fulfilmentLocationName;
    private StockMovementType movementType;
    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private String unitOfMeasure;
    private String referenceType;
    private String referenceId;
    private Long createdByUserId;
    private String notes;
    private Instant createdAt;
}
