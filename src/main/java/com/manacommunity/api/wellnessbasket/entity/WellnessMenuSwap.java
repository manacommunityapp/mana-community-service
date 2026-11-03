package com.manacommunity.api.wellnessbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Resident-requested weekly product swap for a specific delivery date.
 * Requests are only accepted during the Friday 18:00 – Saturday 20:00 IST window.
 */
@Getter
@Setter
@Entity
@Table(name = "wellness_menu_swap", schema = "manacommunity")
public class WellnessMenuSwap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_pref_id", nullable = false)
    private WellnessSubscriberPreference subscriberPreference;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    /** FK to fresh_food_product_config.id – the item being replaced */
    @Column(name = "original_product_id", nullable = false)
    private Long originalProductId;

    /** FK to fresh_food_product_config.id – the requested replacement */
    @Column(name = "requested_product_id", nullable = false)
    private Long requestedProductId;

    /** PENDING | APPROVED | REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
