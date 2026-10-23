package com.manacommunity.api.trip.split.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_expense", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripExpense {

    public static final String ACTIVE = "ACTIVE";
    public static final String UNSPLIT = "UNSPLIT";
    public static final String VOIDED = "VOIDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, length = 50)
    private String tripId;

    @Column(name = "category_code", nullable = false, length = 30)
    private String categoryCode;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "paid_by_user_id", nullable = false)
    private Long paidByUserId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = ACTIVE;

    @Column(name = "split_method", length = 20)
    private String splitMethod;

    @Column(name = "receipt_url", length = 1000)
    private String receiptUrl;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
}
