package com.manacommunity.api.fresh.dispatch.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fresh_delivery_item", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshDeliveryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_batch_id", nullable = false)
    private FreshDeliveryBatch deliveryBatch;

    @Column(name = "commerce_order_id")
    private Long commerceOrderId;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "tower", length = 40)
    private String tower;

    @Column(name = "flat_no", length = 30)
    private String flatNo;

    @Column(name = "sort_sequence", nullable = false)
    @Builder.Default
    private Integer sortSequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "handover_status", nullable = false, length = 40)
    @Builder.Default
    private HandoverStatus handoverStatus = HandoverStatus.PENDING;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "proof_image_url", columnDefinition = "TEXT")
    private String proofImageUrl;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}