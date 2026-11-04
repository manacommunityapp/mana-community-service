package com.manacommunity.api.fresh.inventory.dto;

import com.manacommunity.api.fresh.inventory.model.StockMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordStockMovementRequest {

    @NotNull(message = "Vendor Product ID is required")
    private Long vendorProductId;

    private Long batchId;
    private Long fulfilmentLocationId;

    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    private String unitOfMeasure;
    private String referenceType;
    private String referenceId;
    private Long createdByUserId;
    private String notes;
}
