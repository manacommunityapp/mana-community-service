package com.manacommunity.api.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_equipment", indexes = {
        @Index(name = "idx_booking_equipment_booking", columnList = "booking_id"),
        @Index(name = "idx_booking_equipment_resource", columnList = "resource_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private ResourceBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column
    private Integer quantity;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
