package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EventBudgetRequest {

    @NotNull
    private Long communityId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalPlannedAmount;

    private String notes;

    private List<LineItemRequest> lineItems;

    @Data
    public static class LineItemRequest {
        @NotBlank
        private String category;

        private String description;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal plannedAmount;
    }
}
