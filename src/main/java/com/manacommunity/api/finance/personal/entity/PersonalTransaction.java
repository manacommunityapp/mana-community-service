package com.manacommunity.api.finance.personal.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "PersonalFinanceTransaction")
@Table(name = "personal_finance_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalTransaction {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 20)
    private String type; // INCOME, EXPENSE, TRANSFER

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "₹";

    @Column(name = "category_id", length = 64)
    private String categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "category_icon", length = 100)
    private String categoryIcon;

    @Column(name = "category_color", length = 50)
    private String categoryColor;

    @Column(name = "subcategory_name", length = 100)
    private String subcategoryName;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "to_account_id", length = 64)
    private String toAccountId;

    @Column(name = "to_account_name", length = 100)
    private String toAccountName;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "is_mana_projection", nullable = false)
    @Builder.Default
    private Boolean isManaProjection = false;

    @Column(name = "source_module", length = 50)
    private String sourceModule; // COMMUNITY_FINANCE, MARKETPLACE, FOOD, TRIPS, SPORTS, ACADEMY

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Column(name = "receipt_url", length = 1000)
    private String receiptUrl;

    @Column(length = 500)
    private String tags;

    @Column(name = "split_details", columnDefinition = "TEXT")
    private String splitDetails;

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
