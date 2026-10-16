package com.manacommunity.api.homeservices.instahelp.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insta_help_booking", indexes = {
        @Index(name = "idx_ihb_community", columnList = "community_id"),
        @Index(name = "idx_ihb_status", columnList = "status"),
        @Index(name = "idx_ihb_booking_ref", columnList = "booking_ref")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstaHelpBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_ref", unique = true, nullable = false, length = 20)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(length = 100)
    private String subtask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Duration duration;

    @Column(name = "price_amount", precision = 10, scale = 2)
    private BigDecimal priceAmount;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "closure_otp", length = 6)
    private String closureOtp;

    @Column(name = "helper_name", length = 100)
    private String helperName;

    @Column(name = "helper_rating")
    private Double helperRating;

    @Column(name = "helper_eta_minutes")
    private Integer helperEtaMinutes;

    @Column(name = "helper_distance_label", length = 100)
    private String helperDistanceLabel;

    @Column(name = "resident_rating")
    private Integer residentRating;

    @Column(name = "resident_feedback", length = 500)
    private String residentFeedback;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public enum ServiceType {
        CLEANING, COOKING, MAID, IRONING, LAUNDRY, BABYSITTING
    }

    public enum Duration {
        ONE_HOUR, TWO_HOURS, FOUR_HOURS, FULL_DAY
    }

    public enum BookingStatus {
        PENDING, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
