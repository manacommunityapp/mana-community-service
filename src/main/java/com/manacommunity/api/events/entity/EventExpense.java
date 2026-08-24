package com.manacommunity.api.events.entity;


import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_expense", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityEvent event;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ExpenseStatus expenseStatus = ExpenseStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_name", length = 200)
    private String createdByName;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ExpenseStatus {
        PENDING, APPROVED, REJECTED, PAID
    }


}
