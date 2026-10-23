package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_seller_profiles", indexes = {
    @Index(name = "idx_mkt_seller_comm", columnList = "community_id"),
    @Index(name = "idx_mkt_seller_kyc", columnList = "kyc_status"),
    @Index(name = "idx_mkt_seller_user", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSellerProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", nullable = false, length = 50)
    @Builder.Default
    private SellerType sellerType = SellerType.HOMEPRENEUR;

    @Column(name = "store_description", length = 2000)
    private String storeDescription;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Column(name = "fssai_license_number", length = 50)
    private String fssaiLicenseNumber;

    @Column(name = "gstin", length = 50)
    private String gstin;

    @Column(name = "pan_number", length = 50)
    private String panNumber;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 30)
    private String bankIfscCode;

    @Column(name = "bank_account_holder_name", length = 150)
    private String bankAccountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 50)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.UNVERIFIED;

    @Column(name = "kyc_rejection_reason", length = 1000)
    private String kycRejectionReason;

    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_open")
    @Builder.Default
    private Boolean isOpen = true;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.00);

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_orders_completed")
    @Builder.Default
    private Integer totalOrdersCompleted = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    public enum SellerType {
        HOMEPRENEUR,
        LOCAL_MERCHANT,
        COMMUNITY_FARMER,
        RESIDENT_SELLER
    }

    public enum KycStatus {
        UNVERIFIED,
        SUBMITTED,
        UNDER_REVIEW,
        VERIFIED,
        REJECTED,
        SUSPENDED
    }
}
