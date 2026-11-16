package com.manacommunity.api.fresh.inventory.dto;

import com.manacommunity.api.fresh.inventory.model.WastageCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordWastageRequest {

    @NotNull(message = "Vendor Product ID is required")
    private Long vendorProductId;

    private Long batchId;
    private Long fulfilmentLocationId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private String unitOfMeasure;
    private BigDecimal costPricePerUnit;

    @NotNull(message = "Wastage category is required")
    private WastageCategory wastageCategory;

    private String notes;
    private String disposalMethod;

    @NotNull(message = "Recorded by user ID is required")
    private Long recordedByUserId;
}
