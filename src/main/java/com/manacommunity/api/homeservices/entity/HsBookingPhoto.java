package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "hs_booking_photo") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsBookingPhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_id", nullable = false) private Long bookingId;
    @Column(nullable = false, columnDefinition = "hs_photo_stage") private String stage;
    @Column(name = "photo_url", nullable = false, length = 1024) private String photoUrl;
    @Column(name = "uploaded_by", nullable = false) private Long uploadedBy;
    @Column(length = 500) private String caption;
    @Column(name = "uploaded_at") private Instant uploadedAt = Instant.now();
}
