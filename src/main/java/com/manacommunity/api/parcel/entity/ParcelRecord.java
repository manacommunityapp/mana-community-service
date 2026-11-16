package com.manacommunity.api.parcel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcel_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_no", nullable = false, length = 30)
    private String flatNo;

    @Column(name = "resident_user_id")
    private Long residentUserId;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "received_by_guard_id")
    private Long receivedByGuardId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "AWAITING_PICKUP";

    @Column(name = "collection_otp", length = 10)
    private String collectionOtp;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "collected_by_user_id")
    private Long collectedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
