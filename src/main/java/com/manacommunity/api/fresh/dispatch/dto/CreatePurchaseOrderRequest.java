package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.DemandSource;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class CreatePurchaseOrderRequest {
    private Long vendorId;
    private LocalDate targetDeliveryDate;
    private String notes;
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        private Long vendorProductId;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private DemandSource demandSource;
    }
}