package com.manacommunity.api.booking.entity;

import com.manacommunity.api.booking.entity.enums.BookingType;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "resource", indexes = {
        @Index(name = "idx_resource_community", columnList = "community_id"),
        @Index(name = "idx_resource_category", columnList = "category_id"),
        @Index(name = "idx_resource_status", columnList = "status"),
        @Index(name = "idx_resource_community_status", columnList = "community_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ResourceCategory category;

    @Column(length = 1000)
    private String description;

    @Column
    private Integer capacity;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String building;

    @Column(length = 50)
    private String floor;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "booking_duration_minutes")
    private Integer bookingDurationMinutes;

    @Column(name = "minimum_duration_minutes")
    private Integer minimumDurationMinutes;

    @Column(name = "maximum_duration_minutes")
    private Integer maximumDurationMinutes;

    @Column(name = "buffer_time_minutes")
    private Integer bufferTimeMinutes;

    @Column(name = "cleaning_time_minutes")
    private Integer cleaningTimeMinutes;

    @Column(name = "advance_booking_days")
    private Integer advanceBookingDays;

    @Column(name = "max_bookings_per_user")
    private Integer maxBookingsPerUser;

    @Column(name = "max_active_bookings")
    private Integer maxActiveBookings;

    @Column(name = "cancellation_hours")
    private Integer cancellationHours;

    @Column(name = "auto_cancel")
    @Builder.Default
    private boolean autoCancel = false;

    @Column(name = "approval_required")
    @Builder.Default
    private boolean approvalRequired = false;

    @Column(name = "deposit_required")
    @Builder.Default
    private boolean depositRequired = false;

    @Column(name = "payment_required")
    @Builder.Default
    private boolean paymentRequired = false;

    @Column(name = "allow_waitlist")
    @Builder.Default
    private boolean allowWaitlist = false;

    @Column(name = "allow_guest")
    @Builder.Default
    private boolean allowGuest = false;

    @Column(name = "qr_check_in")
    @Builder.Default
    private boolean qrCheckIn = false;

    @Column(name = "recurring_booking_allowed")
    @Builder.Default
    private boolean recurringBookingAllowed = false;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", length = 20)
    @Builder.Default
    private BookingType bookingType = BookingType.SLOT_BASED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResourceStatus status = ResourceStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "deleted")
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
