package com.manacommunity.api.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_handover_passes", indexes = {
    @Index(name = "idx_mkt_pass_order", columnList = "order_id", unique = true),
    @Index(name = "idx_mkt_pass_qr", columnList = "pass_qr_code", unique = true),
    @Index(name = "idx_mkt_pass_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketHandoverPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"handoverPass"})
    private MarketOrder order;

    @Column(name = "pass_qr_code", unique = true, nullable = false, length = 100)
    private String passQrCode;

    @Column(name = "otp_pin_hash", nullable = false, length = 255)
    private String otpPinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PassStatus status = PassStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_guard_id")
    private AppUser verifiedByGuard;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = PassStatus.ACTIVE;
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusHours(48);
    }

    public enum PassStatus { ACTIVE, VERIFIED, EXPIRED, CANCELLED }
}
