package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flat_tab_accounts", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class FlatTabAccountEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "credit_limit_paise", nullable = false)
    private long creditLimitPaise = 1_000_000L;

    @Column(name = "unbilled_tab_paise", nullable = false)
    private long unbilledTabPaise = 0L;

    @Column(name = "active_holds_paise", nullable = false)
    private long activeHoldsPaise = 0L;

    @Column(name = "offline_floor_cap_paise", nullable = false)
    private long offlineFloorCapPaise = 150_000L;

    @Column(name = "billing_preference", nullable = false, length = 40)
    private String billingPreference = "WALLET_WITH_TAB_OVERFLOW";

    @Column(name = "overdue_frozen", nullable = false)
    private boolean overdueFrozen = false;

    @Column(name = "pin_hash", nullable = false, length = 128)
    private String pinHash;

    @Column(name = "pin_failed_attempts", nullable = false)
    private int pinFailedAttempts = 0;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public long availableCreditPaise() {
        return creditLimitPaise - (unbilledTabPaise + activeHoldsPaise);
    }
}
