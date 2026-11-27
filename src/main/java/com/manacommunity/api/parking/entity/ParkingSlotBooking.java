package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "parking_slot_booking",
       indexes = {
           @Index(name = "idx_psb_date", columnList = "community_id,booking_date,status"),
           @Index(name = "idx_psb_user", columnList = "booked_by")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ParkingSlotBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot;

    @Column(name = "booked_by", nullable = false)
    private Long bookedBy;

    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Column(name = "guest_vehicle", length = 30)
    private String guestVehicle;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum BookingStatus { CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED }
}
