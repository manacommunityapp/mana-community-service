package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "vms_booking_reschedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsBookingReschedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private VmsBooking booking;

    @Column(name = "original_date", nullable = false)
    private LocalDate originalDate;

    @Column(name = "original_time")
    private LocalTime originalTime;

    @Column(name = "new_date", nullable = false)
    private LocalDate newDate;

    @Column(name = "new_time")
    private LocalTime newTime;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private AppUser requestedBy;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
