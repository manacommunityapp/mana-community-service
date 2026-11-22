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

    /**
     * GSTIN, in the clear and deliberately so.
     *
     * <p>Unlike PAN and the bank account this is a <em>published</em> identifier: the GST
     * department exposes every GSTIN, with the registered trade name and address, through
     * public "Search Taxpayer" lookup. Hashing it would destroy the only thing it is for —
     * an admin reading it off the review screen and checking it against that public registry
     * — while protecting nothing that is not already public.
     *
     * <p>Caveat worth knowing: characters 3-12 of a GSTIN <em>are</em> the holder's PAN, so
     * for a GST-registered seller storing the GSTIN in the clear also reveals the PAN. That
     * does not argue for encrypting the GSTIN (the mapping is public either way); it means
     * the PAN protection below buys real confidentiality only for sellers who are not
     * GST-registered, which is most homepreneurs.
     */
    @Column(name = "gstin", length = 50)
    private String gstin;

    /**
     * HMAC-SHA256(server pepper, client SHA-256 of the normalised PAN) — never the PAN.
     *
     * <p>PAN identifies a person and nothing in this service ever needs it in the clear: it
     * is collected so a human can confirm the seller is who they claim, and a digest plus the
     * last four characters does that. Same shape and same protector as resident KYC
     * ({@code KycIdNumberProtector}); 100 chars matches {@code kyc_document.id_number_hash}.
     */
    @Column(name = "pan_number_hash", length = 100)
    private String panNumberHash;

    /** Last 1-4 characters of the PAN. Display only — the UI renders the rest masked. */
    @Column(name = "pan_number_last4", length = 4)
    private String panNumberLast4;

    /**
     * Settlement account number, AES-256-GCM encrypted at rest by
     * {@link com.manacommunity.api.security.EncryptedStringConverter}.
     *
     * <p>Not hashed: a payout has to send the actual digits to the bank, so a one-way digest
     * would destroy the field's only purpose. Encryption keeps it usable while a database
     * dump on its own yields nothing. The column is TEXT (V507) because Base64(IV + ciphertext
     * + GCM tag) is roughly 4/3*(len + 28) characters and would not fit the old VARCHAR(50) —
     * the same mistake V506 had to fix on the health columns.
     *
     * <p>Never put this on a response DTO; {@link #bankAccountLast4} is what any UI gets.
     */
    @Convert(converter = com.manacommunity.api.security.EncryptedStringConverter.class)
    @Column(name = "bank_account_number", columnDefinition = "TEXT")
    private String bankAccountNumber;

    /**
     * Last 4 digits of the settlement account, stored separately in the clear so masked
     * display and the admin review list never have to decrypt the full number.
     */
    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

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
