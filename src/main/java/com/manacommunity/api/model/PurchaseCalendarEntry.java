package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_calendar_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseCalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ProcurementCategory category = ProcurementCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalendarFrequency frequency;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String unit = "units";

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "estimated_budget", precision = 14, scale = 2)
    private BigDecimal estimatedBudget;

    @Column(name = "delivery_location", length = 300)
    private String deliveryLocation;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_rfq_date")
    private LocalDate lastRfqDate;

    @Column(name = "last_rfq_id")
    private Long lastRfqId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(length = 500)
    private String notes;

    @Column(name = "preferred_supplier_ids", length = 500)
    private String preferredSupplierIds;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
