package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "voucher_redemptions", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class VoucherRedemptionEntity {

    @Id
    @Column(name = "vid", nullable = false, length = 100)
    private String vid;

    @Column(name = "uid", nullable = false, length = 100)
    private String uid;

    @Column(name = "kiosk_id", nullable = false, length = 100)
    private String kioskId;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "is_conflicted", nullable = false)
    private boolean conflicted = false;
}
