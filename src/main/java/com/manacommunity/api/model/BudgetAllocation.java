package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_allocations",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget_financial_year_category",
                columnNames = {"financial_year", "category"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExpenseCategory category;

    @Column(name = "allocated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "spent_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getAvailableAmount() {
        return allocatedAmount.subtract(spentAmount);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
