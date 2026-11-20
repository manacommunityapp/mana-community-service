package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "delivery_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long templateId;
    private Long vendorId;
    private Long communityId;
    private LocalDate slotDate;
    private Instant slotStart;
    private Instant slotEnd;
    private Instant cutoffAt;
    private int capacity;
    private int bookedCount;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "slot_status")
    private SlotStatus status;
    private Instant createdAt;

    public enum SlotStatus { OPEN, FULL, CUTOFF_PASSED, CANCELLED }
}
