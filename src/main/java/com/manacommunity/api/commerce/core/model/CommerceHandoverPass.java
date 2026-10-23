package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "commerce_handover_pass")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceHandoverPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @Column(name = "pass_code", unique = true, nullable = false, length = 64)
    private String passCode;

    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false)
    private String qrPayload;

    @Column(nullable = false, length = 8)
    private String otp;

    @Column(name = "fulfillment_type", length = 32, nullable = false)
    private String fulfillmentType;

    @Column(name = "pickup_point", length = 150)
    private String pickupPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private AppUser verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(length = 32, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
}