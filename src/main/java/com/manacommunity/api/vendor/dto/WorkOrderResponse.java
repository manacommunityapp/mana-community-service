package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkOrderResponse {
    private Long id;
    private String workOrderNumber;
    private String title;
    private String description;
    private String type;
    private String priority;
    private String status;
    private VendorRef vendor;
    private String location;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String completionNotes;
    private List<WorkOrderItemResponse> items;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
    }

    @Data
    @Builder
    public static class WorkOrderItemResponse {
        private Long id;
        private String description;
        private Integer quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String status;
    }
}
