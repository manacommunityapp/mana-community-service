package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_buy_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private GroupDeal deal;

    @Column(name = "deal_title", nullable = false)
    private String dealTitle;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "savings_amount", precision = 12, scale = 2)
    private BigDecimal savingsAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CONFIRMED;

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "PAID";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp;

    @Column(name = "qr_token", unique = true, length = 100)
    private String qrToken;

    @Column(name = "pickup_point")
    private String pickupPoint;

    @Column(name = "pickup_date")
    private LocalDateTime pickupDate;

    @Column(name = "escrow_hold_amount", precision = 12, scale = 2)
    private BigDecimal escrowHoldAmount;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_otp", length = 10)
    private String deliveryOtp;

    @Column(name = "delivery_partner_name", length = 100)
    private String deliveryPartnerName;

    @Column(name = "delivery_partner_phone", length = 30)
    private String deliveryPartnerPhone;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @Column(name = "delivery_timestamp")
    private LocalDateTime deliveryTimestamp;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "refund_timestamp")
    private LocalDateTime refundTimestamp;

    @Column(name = "tier_price_refund_amount", precision = 12, scale = 2)
    private BigDecimal tierPriceRefundAmount;

    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "is_settled")
    @Builder.Default
    private Boolean isSettled = false;

    @Column(name = "special_notes")
    private String specialNotes;

    @Column(name = "collector_pin", length = 10)
    private String collectorPin;

    @Column(name = "collector_name", length = 100)
    private String collectorName;

    @Column(name = "collector_relation", length = 50)
    private String collectorRelation;
}
