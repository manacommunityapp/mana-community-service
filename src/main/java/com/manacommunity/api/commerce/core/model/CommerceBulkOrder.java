package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_bulk_order")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommerceBulkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bulk_order_code", nullable = false, unique = true, length = 50)
    private String bulkOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private AppUser customerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_user_id")
    private AppUser vendorUser;

    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "quoted_amount", precision = 14, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "final_amount", precision = 14, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "vendor_note", columnDefinition = "TEXT")
    private String vendorNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_order_id")
    private CommerceOrder fulfilledOrder;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
