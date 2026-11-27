package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "voucher_claims", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class VoucherClaimEntity {

    @Id
    @Column(name = "vid", nullable = false, length = 100)
    private String vid;

    @Column(name = "voucher_id", nullable = false, length = 100)
    private String voucherId;

    @Column(name = "cid", nullable = false, length = 100)
    private String cid;

    @Column(name = "uid", nullable = false, length = 100)
    private String uid;

    @Column(name = "sid", nullable = false, length = 100)
    private String sid;

    @Column(name = "nonce", nullable = false, length = 32)
    private String nonce;

    @Column(name = "hmac_suffix", nullable = false, length = 128)
    private String hmacSuffix;

    @Column(name = "totp_secret_encrypted", columnDefinition = "TEXT", nullable = false)
    private String totpSecretEncrypted;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "CLAIMED";

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;
}
