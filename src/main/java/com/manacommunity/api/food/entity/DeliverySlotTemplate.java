package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.Instant;

@Entity
@Table(name = "delivery_slot_template")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySlotTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long vendorId;
    private Long communityId;
    private int dayOfWeek;
    private LocalTime slotStart;
    private LocalTime slotEnd;
    private int capacity;
    private int cutoffHours;
    private boolean isActive;
    private Instant createdAt;
}
