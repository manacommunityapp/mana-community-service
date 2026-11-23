package com.manacommunity.api.fulfillment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_fulfillment_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderFulfillmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "order_type", nullable = false, length = 50)
    private String orderType;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
