package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_rotation", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyRotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private KittyPartyCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_user_id", nullable = false)
    private AppUser beneficiaryUser;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "rotation_status", nullable = false, length = 20)
    @Builder.Default
    private String rotationStatus = "PLANNED"; // PLANNED, DISBURSED, SKIPPED, SWAPPED

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "external_payout_ref", length = 150)
    private String externalPayoutRef;

    @Column(name = "payout_receipt_url", columnDefinition = "TEXT")
    private String payoutReceiptUrl;
}
