package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Additional per-community configuration not stored in the base Community
 * entity (which has minimal fields in the original schema).
 *
 * Relationship: one CommunitySettings row per Community (created lazily on
 * first admin settings save; GET returns defaults if row is absent).
 */
@Entity
@Table(name = "community_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunitySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false, unique = true)
    private Community community;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String address;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 500;

    // ── Feature toggles ──────────────────────────────────────────────
    @Column(name = "feature_marketplace")
    @Builder.Default
    private Boolean featureMarketplace = true;

    @Column(name = "feature_sports")
    @Builder.Default
    private Boolean featureSports = true;

    @Column(name = "feature_auction")
    @Builder.Default
    private Boolean featureAuction = true;

    @Column(name = "feature_jobs")
    @Builder.Default
    private Boolean featureJobs = true;

    @Column(name = "feature_polls")
    @Builder.Default
    private Boolean featurePolls = true;

    // ── Accounting freeze ────────────────────────────────────────────────
    @Column(name = "accounting_freeze_date")
    private LocalDate accountingFreezeDate;

    @Column(name = "accounting_freeze_note", length = 500)
    private String accountingFreezeNote;

    // ── Payment configuration ────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_payment_gateways", columnDefinition = "VARCHAR(20)[]")
    @Builder.Default
    private String[] allowedPaymentGateways = new String[]{"CASHFREE"};

    @Column(name = "manual_payment_enabled", nullable = false)
    @Builder.Default
    private boolean manualPaymentEnabled = true;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "manual_payment_modes", columnDefinition = "VARCHAR(20)[]")
    @Builder.Default
    private String[] manualPaymentModes = new String[]{"CASH", "UPI"};

    @Column(name = "payment_instructions", length = 1000)
    private String paymentInstructions;

    @Column(name = "default_gateway", length = 20)
    private String defaultGateway;

    @Column(name = "cashier_can_record_payment", nullable = false)
    @Builder.Default
    private boolean cashierCanRecordPayment = true;

    @Column(name = "fallback_gateway", length = 20)
    private String fallbackGateway;
}
