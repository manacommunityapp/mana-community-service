package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_performance_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierPerformanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "rfq_id")
    private Long rfqId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PerformanceEventType eventType;

    @Column(name = "numeric_value", precision = 5, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "on_time_flag")
    private Boolean onTimeFlag;

    @Column(name = "notes")
    private String notes;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
