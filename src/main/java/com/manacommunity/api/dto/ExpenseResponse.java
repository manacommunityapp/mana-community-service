package com.manacommunity.api.dto;

import com.manacommunity.api.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API view of an {@link Expense}. Exposes only presentation-safe fields
 * (the creator is flattened to their display name, no entity graph leaks).
 */
public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal amount,
        String category,
        String description,
        String receiptFileName,
        String receiptUrl,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getTitle(),
                e.getAmount(),
                e.getCategory(),
                e.getDescription(),
                e.getReceiptFileName(),
                e.getReceiptUrl(),
                e.getStatus(),
                e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
