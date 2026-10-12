package com.manacommunity.api.commerce.customer.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_customer_profile",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "community_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceCustomerProfile extends BaseAuditEntity {

    public enum CustomerStatus { ACTIVE, INACTIVE, BLACKLISTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus customerStatus = CustomerStatus.ACTIVE;

    @Column(name = "loyalty_points", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal loyaltyPoints = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_spend", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalSpend = BigDecimal.ZERO;

    @Column(name = "is_member", nullable = false)
    @Builder.Default
    private Boolean isMember = false;

    /** Comma-separated preferred category slugs or names. */
    @Column(name = "preferred_categories", columnDefinition = "TEXT")
    private String preferredCategories;

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommerceCustomerAddress> addresses = new ArrayList<>();

    /** Points to the default address (kept as a bare FK id to avoid circular cascade). */
    @Column(name = "default_address_id")
    private Long defaultAddressId;
}
