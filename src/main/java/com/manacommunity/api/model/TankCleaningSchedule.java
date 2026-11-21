package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tank_cleaning_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TankCleaningSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tank_id", nullable = false)
    private Long tankId;

    @Column(name = "last_cleaned_date", nullable = false)
    private LocalDate lastCleanedDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "cleaning_type", nullable = false, length = 60)
    @Builder.Default
    private String cleaningType = "MECHANIZED_DEWATERING_UV"; // MECHANIZED_DEWATERING_UV, CHEMICAL_DISINFECTION, MANUAL_SCRUB

    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, OVERDUE

    @Column(name = "certificate_url")
    private String certificateUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
