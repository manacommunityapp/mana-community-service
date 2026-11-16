package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventBudgetResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long communityId;
    private BigDecimal totalPlannedAmount;
    private BigDecimal totalActualAmount;
    private BigDecimal variance;
    private String currency;
    private String notes;
    private List<LineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class LineItemResponse {
        private Long id;
        private String category;
        private String description;
        private BigDecimal plannedAmount;
        private LocalDateTime createdAt;
    }
}
