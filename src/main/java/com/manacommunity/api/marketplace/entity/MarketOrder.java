package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_orders", indexes = {
    @Index(name = "idx_mkt_order_num", columnList = "order_number", unique = true),
    @Index(name = "idx_mkt_order_buyer", columnList = "buyer_id"),
    @Index(name = "idx_mkt_order_seller", columnList = "seller_id"),
    @Index(name = "idx_mkt_order_status", columnList = "status"),
    @Index(name = "idx_mkt_order_comm", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", length = 30)
    @Builder.Default
    private DeliveryMode deliveryMode = DeliveryMode.DOORSTEP;

    @Column(name = "delivery_address", length = 250)
    private String deliveryAddress;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarketOrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private MarketHandoverPass handoverPass;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (deliveryFee == null) deliveryFee = BigDecimal.ZERO;
        if (paymentMethod == null) paymentMethod = PaymentMethod.UPI;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PAID;
        if (deliveryMode == null) deliveryMode = DeliveryMode.DOORSTEP;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus { PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED, REFUNDED }
    public enum PaymentMethod { UPI, CARD, NET_BANKING, CASH_ON_HANDOVER, WALLET }
    public enum PaymentStatus { PENDING, PAID, REFUNDED, FAILED }
    public enum DeliveryMode { DOORSTEP, CLUBHOUSE_PICKUP, GATE_HANDOVER }
}
