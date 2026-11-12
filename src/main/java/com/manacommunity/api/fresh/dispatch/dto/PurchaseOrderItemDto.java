package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.DemandSource;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class PurchaseOrderItemDto {
    private Long id;
    private Long vendorProductId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private DemandSource demandSource;
}