package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_expense_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrExpenseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "expense_claim_id", nullable = false)
    private HrExpenseClaim expenseClaim;
    @Column(name = "description", nullable = false, length = 300) private String description;
    @Column(name = "amount", nullable = false) private Double amount;
    @Column(name = "expense_date") private LocalDate expenseDate;
    @Column(name = "receipt_url", length = 500) private String receiptUrl;
    @Column(name = "category", length = 100) private String category;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
