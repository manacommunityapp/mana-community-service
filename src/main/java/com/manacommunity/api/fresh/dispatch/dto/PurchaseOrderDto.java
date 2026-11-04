package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.DemandSource;
import com.manacommunity.api.fresh.dispatch.model.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class PurchaseOrderDto {
    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorBusinessName;
    private LocalDate targetDeliveryDate;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private Instant createdAt;
    private List<PurchaseOrderItemDto> items;
}