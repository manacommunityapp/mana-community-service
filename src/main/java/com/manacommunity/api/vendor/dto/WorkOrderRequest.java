package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkOrderRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private String type;
    @NotNull
    private String priority;
    private Long vendorId;
    private Long bookingId;
    private Long contractId;
    private String location;
    private LocalDate scheduledDate;
    private BigDecimal estimatedCost;
    private List<WorkOrderItemRequest> items;

    @Data
    public static class WorkOrderItemRequest {
        private String description;
        private Integer quantity;
        private BigDecimal unitCost;
    }
}
