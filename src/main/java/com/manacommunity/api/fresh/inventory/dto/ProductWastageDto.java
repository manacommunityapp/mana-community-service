package com.manacommunity.api.fresh.inventory.dto;

import com.manacommunity.api.fresh.inventory.model.WastageCategory;
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
public class ProductWastageDto {
    private Long id;
    private String wastageReference;
    private Long vendorProductId;
    private String productName;
    private Long batchId;
    private String batchNumber;
    private Long fulfilmentLocationId;
    private String fulfilmentLocationName;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal costPricePerUnit;
    private BigDecimal totalLossAmount;
    private WastageCategory wastageCategory;
    private String notes;
    private String disposalMethod;
    private Long recordedByUserId;
    private String recordedByUserName;
    private Instant recordedAt;
}
