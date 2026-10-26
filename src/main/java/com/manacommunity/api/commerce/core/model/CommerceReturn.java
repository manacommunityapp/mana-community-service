package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_return")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommerceReturn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_code", nullable = false, unique = true, length = 50)
    private String returnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private AppUser customerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "return_type", nullable = false, length = 20)
    @Builder.Default
    private String returnType = "RETURN";

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private AppUser resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_order_id")
    private CommerceOrder exchangeOrder;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
