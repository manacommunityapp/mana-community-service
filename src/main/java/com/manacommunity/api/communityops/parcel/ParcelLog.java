package com.manacommunity.api.communityops.parcel;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "parcel_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "resident_user_id")
    private Long residentUserId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "courier_name")
    private String courierName;

    @Column(name = "tracking_number")
    private String trackingNumber;

    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParcelStatus status = ParcelStatus.RECEIVED;

    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @Builder.Default
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "picked_up_by")
    private Long pickedUpBy;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "pickup_otp")
    private String pickupOtp;

    @Column(name = "image_key")
    private String imageKey;

    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
