package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_timeline_event")
@Data
public class PaymentTimelineEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "source_module", nullable = false, length = 100) private String sourceModule;
    @Column(name = "reference_id", nullable = false) private Long referenceId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    @Column(length = 500) private String description;
    @Column(columnDefinition = "jsonb") private String metadata;
    @Column(name = "occurred_at") private Instant occurredAt = Instant.now();
}
