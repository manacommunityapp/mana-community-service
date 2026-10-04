package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_buy_order_disputes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDispute extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_number", unique = true, nullable = false, length = 50)
    private String disputeNumber;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "deal_id", length = 50)
    private String dealId;

    @Column(name = "deal_title", length = 200)
    private String dealTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "resident_name", length = 100)
    private String residentName;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(name = "requested_resolution", length = 50)
    private String requestedResolution;

    @Column(name = "claim_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal claimAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "vendor_response", columnDefinition = "TEXT")
    private String vendorResponse;
}
