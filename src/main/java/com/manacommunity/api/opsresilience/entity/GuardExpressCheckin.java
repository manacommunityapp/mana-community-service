package com.manacommunity.api.opsresilience.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "guard_express_checkins",
    uniqueConstraints = @UniqueConstraint(name = "uq_guard_express_gate_seq", columnNames = {"gate_id", "client_seq_id"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GuardExpressCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "gate_id", nullable = false, length = 64)
    private String gateId;

    @Column(name = "guard_id", nullable = false)
    private Long guardId;

    @Column(name = "client_seq_id", nullable = false)
    private Long clientSeqId;

    @Column(name = "entry_mode", nullable = false, length = 32)
    private String entryMode; // COURIER_ICON, OTP_6_DIGIT, QR_PASS, GUARD_OVERRIDE_PHOTO

    @Column(name = "brand_code", length = 32)
    private String brandCode;

    @Column(name = "flat_no", nullable = false, length = 32)
    private String flatNo;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "audio_cue", nullable = false, length = 32)
    private String audioCue;

    @Builder.Default
    @Column(name = "requires_mc_review", nullable = false)
    private boolean requiresMcReview = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
