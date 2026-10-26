package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainer_slot_booking",
        indexes = {
                @Index(name = "idx_tsb_slot", columnList = "slot_id"),
                @Index(name = "idx_tsb_user", columnList = "user_id"),
                @Index(name = "idx_tsb_trainer", columnList = "trainer_id"),
                @Index(name = "idx_tsb_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSlotBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TrainerSlot slot;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    @Column(length = 20)
    private String tower;

    @Column(name = "flat_number", length = 20)
    private String flatNumber;

    /** CONFIRMED | CANCELLED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "booked_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
