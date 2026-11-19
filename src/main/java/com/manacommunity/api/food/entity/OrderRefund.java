package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_refund")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long communityId;
    private Long orderId;
    private String orderType;
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "refund_trigger", name = "trigger_type")
    private RefundTrigger triggerType;
    private BigDecimal amount;
    private String itemDetails;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "refund_status")
    private RefundStatus status;
    private boolean autoApproved;
    private Long reviewedBy;
    private Instant reviewedAt;
    private Instant processedAt;
    private String notes;
    private Instant createdAt;

    public enum RefundTrigger { OUT_OF_STOCK, SUBSTITUTION_REJECTED, LATE_DELIVERY, QUALITY_ISSUE, WRONG_ITEM, CANCELLED_BY_VENDOR, MANUAL }
    public enum RefundStatus { INITIATED, APPROVED, PROCESSED, REJECTED }
}
