package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.HandlingClass;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class CreateDeliveryBatchRequest {
    private Long communityId;
    private LocalDate deliveryDate;
    private HandlingClass handlingClass;
    private Long assignedRunnerId;
    private List<DeliveryItemRequest> items;

    @Data
    public static class DeliveryItemRequest {
        private Long commerceOrderId;
        private String customerName;
        private String tower;
        private String flatNo;
        private Integer sortSequence;
    }
}