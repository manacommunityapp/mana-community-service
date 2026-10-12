package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commerce_commission_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceCommissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /** null = applies to all vendors; non-null = vendor-specific override */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_user_id")
    private AppUser vendorUser;

    /** null = applies to all categories */
    @Column(length = 100)
    private String category;

    /** null = applies to all channels */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CommerceChannel channel;

    @Column(name = "fee_percent", nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal feePercent = BigDecimal.valueOf(2.5);

    @Column(name = "gst_on_fee", nullable = false)
    @Builder.Default
    private boolean gstOnFee = false;

    @Column(name = "tds_percent", precision = 6, scale = 4)
    private BigDecimal tdsPercent;

    /** Lower number = higher priority */
    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
