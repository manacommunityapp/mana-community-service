package com.manacommunity.api.fresh.fulfilment.dto;

import com.manacommunity.api.fresh.fulfilment.model.FulfilmentItemStatus;
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
public class FulfilmentItemDto {
    private Long id;
    private Long fulfilmentGroupId;
    private Long orderItemId;
    private Long vendorProductId;
    private String productName;
    private Long batchId;
    private String batchNumber;
    private BigDecimal requestedQuantity;
    private BigDecimal pickedQuantity;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal billedPrice;
    private Boolean isWeightAdjusted;
    private FulfilmentItemStatus status;
    private Instant createdAt;
}
