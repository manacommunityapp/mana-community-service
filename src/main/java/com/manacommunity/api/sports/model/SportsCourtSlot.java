package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_court_slot",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_court_slot_time", columnNames = {"court_id", "slot_date", "start_time"})
    },
    indexes = {
        @Index(name = "idx_court_slot_lookup", columnList = "court_id, slot_date, status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsCourtSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    @JsonIgnore
    private SportsCourt court;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    /**
     * AVAILABLE, HELD, BOOKED, BLOCKED, MAINTENANCE
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "held_until")
    private LocalDateTime heldUntil;

    @Column(name = "held_by_user_id")
    private Long heldByUserId;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "resident_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal residentPrice = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpiredHold() {
        return "HELD".equalsIgnoreCase(this.status) &&
               this.heldUntil != null &&
               LocalDateTime.now().isAfter(this.heldUntil);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
