package com.manacommunity.api.finance.personal.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "PersonalFinanceBudget")
@Table(name = "personal_finance_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalBudget {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "category_id", nullable = false, length = 64)
    private String categoryId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String period = "MONTHLY"; // MONTHLY, QUARTERLY, YEARLY

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "alert_threshold", nullable = false)
    @Builder.Default
    private Integer alertThreshold = 80;

    @Column(nullable = false, length = 10)
    private String month; // YYYY-MM

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
