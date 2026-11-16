package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyExpenseDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitExpenseRequest {
        @NotNull(message = "Expense ID is required")
        private Long expenseId;

        @NotEmpty(message = "Participant user IDs are required")
        private List<Long> participantUserIds;

        private BigDecimal customShareAmount; // if null, divided equally
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettleShareRequest {
        @NotNull(message = "Settlement reference is required")
        private String settlementRef;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseShareResponse {
        private Long id;
        private Long expenseId;
        private String expenseDescription;
        private BigDecimal totalExpenseAmount;
        private Long userId;
        private String userName;
        private BigDecimal shareAmount;
        private String settlementStatus;
        private LocalDateTime settledAt;
        private String settlementRef;
    }
}
