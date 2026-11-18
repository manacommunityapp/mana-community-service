package com.manacommunity.api.payments.config.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "platform_payment_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPaymentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashfree_enabled", nullable = false)
    @Builder.Default
    private boolean cashfreeEnabled = true;

    @Column(name = "razorpay_enabled", nullable = false)
    @Builder.Default
    private boolean razorpayEnabled = false;

    @Column(name = "mana_pay_enabled", nullable = false)
    @Builder.Default
    private boolean manaPayEnabled = false;

    @Column(name = "bbps_enabled", nullable = false)
    @Builder.Default
    private boolean bbpsEnabled = false;

    @Column(name = "default_gateway", nullable = false, length = 20)
    @Builder.Default
    private String defaultGateway = "CASHFREE";

    /** Convenience fee applied on top of base amount. 0 means no fee. Never float. */
    @Column(name = "convenience_fee_percent", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal convenienceFeePercent = BigDecimal.ZERO;

    /** Null = no platform-wide limit. */
    @Column(name = "max_transaction_amount", precision = 14, scale = 2)
    private BigDecimal maxTransactionAmount;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
