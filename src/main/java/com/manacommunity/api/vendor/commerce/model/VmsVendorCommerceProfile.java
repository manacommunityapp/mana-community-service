package com.manacommunity.api.vendor.commerce.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_vendor_commerce_profile",
    uniqueConstraints = @UniqueConstraint(name = "uq_vendor_commerce_profile", columnNames = "vms_vendor_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VmsVendorCommerceProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "commerce_status", nullable = false, length = 30)
    @Builder.Default
    private CommerceVendorStatus commerceStatus = CommerceVendorStatus.DRAFT;

    // ── Payment ──────────────────────────────────────────────────────────
    @Column(name = "upi_enabled", nullable = false)
    @Builder.Default
    private Boolean upiEnabled = false;

    @Column(name = "upi_id", length = 80)
    private String upiId;

    // ── Delivery ─────────────────────────────────────────────────────────
    @Column(name = "delivery_available", nullable = false)
    @Builder.Default
    private Boolean deliveryAvailable = false;

    @Column(name = "delivery_radius_km")
    @Builder.Default
    private Integer deliveryRadiusKm = 5;

    @Column(name = "avg_delivery_time_minutes")
    @Builder.Default
    private Integer avgDeliveryTimeMinutes = 60;

    @Column(name = "min_order_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    // ── Pickup ───────────────────────────────────────────────────────────
    @Column(name = "pickup_available", nullable = false)
    @Builder.Default
    private Boolean pickupAvailable = true;

    @Column(name = "pickup_address", length = 400)
    private String pickupAddress;

    // ── Storefront ───────────────────────────────────────────────────────
    @Column(name = "storefront_enabled", nullable = false)
    @Builder.Default
    private Boolean storefrontEnabled = false;

    @Column(name = "storefront_description", length = 1000)
    private String storefrontDescription;

    @Column(name = "storefront_banner_url", length = 500)
    private String storefrontBannerUrl;

    @Column(name = "community_visible", nullable = false)
    @Builder.Default
    private Boolean communityVisible = false;

    // ── Verification ─────────────────────────────────────────────────────
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public enum CommerceVendorStatus {
        DRAFT,
        PENDING_VERIFICATION,
        VERIFIED,
        SUSPENDED,
        REJECTED,
        INACTIVE
    }
}
