package com.manacommunity.api.vendor.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryBatchDto {
    private String id;
    private String batchNumber;
    private String variantId;
    private String variantName;
    private String productName;
    private String mfgDate;
    private String expiryDate;
    private Integer initialQuantity;
    private Integer remainingQuantity;
    private BigDecimal unitCost;
    private String status;
}
