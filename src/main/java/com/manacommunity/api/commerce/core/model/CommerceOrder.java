package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 40)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommerceChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private AppUser seller;

    @Column(name = "vendor_id", length = 64)
    private String vendorId;

    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "channel_reference_id", length = 64)
    private String channelReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private CommerceOrderStatus status = CommerceOrderStatus.CONFIRMED;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "savings_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal savingsAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 32)
    @Builder.Default
    private String paymentMethod = "UPI";

    @Column(name = "payment_status", length = 32)
    @Builder.Default
    private String paymentStatus = "INITIATED";

    @Column(name = "fulfillment_type", length = 32)
    @Builder.Default
    private String fulfillmentType = "CLUBHOUSE_PICKUP";

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "pickup_point", length = 150)
    private String pickupPoint;

    @Column(name = "pickup_slot", length = 80)
    private String pickupSlot;

    @Column(name = "handover_otp", length = 8)
    private String handoverOtp;

    @Column(name = "qr_token", unique = true, length = 120)
    private String qrToken;

    @Column(name = "is_escrow_locked")
    @Builder.Default
    private Boolean isEscrowLocked = true;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommerceOrderItem> items = new ArrayList<>();
}