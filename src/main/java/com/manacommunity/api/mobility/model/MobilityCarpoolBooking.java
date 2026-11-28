package com.manacommunity.api.mobility.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mobility_carpool_booking", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mobility_carpool_booking_offer_user", columnNames = {"offer_id", "user_id"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityCarpoolBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offer_id", nullable = false)
    private MobilityCarpoolOffer offer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false) private short seats = 1;
    @Column(nullable = false, length = 20) private String status = "CONFIRMED";
    @Builder.Default @Column(name = "total_amount", nullable = false, precision = 10, scale = 2) private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "payment_status", nullable = false, length = 25) private String paymentStatus = "CASH";
    @Column(name = "payment_order_id", length = 64) private String paymentOrderId;
    @Column(name = "payment_ref", length = 120) private String paymentRef;
    @Column(name = "refund_amount", precision = 10, scale = 2) private BigDecimal refundAmount;
    @Builder.Default @Column(name = "guardian_consent_status", nullable = false, length = 20) private String guardianConsentStatus = "NOT_REQUIRED";
    @Column(name = "guardian_name", length = 120) private String guardianName;
    @Column(name = "guardian_phone", length = 25) private String guardianPhone;
    @Column(name = "guardian_consented_at") private Instant guardianConsentedAt;
    @Column(name = "booked_at", nullable = false, updatable = false) private Instant bookedAt = Instant.now();
}
