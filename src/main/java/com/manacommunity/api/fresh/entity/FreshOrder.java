package com.manacommunity.api.fresh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fresh_order", indexes = {
    @Index(name = "idx_fo_community", columnList = "community_id"),
    @Index(name = "idx_fo_resident",  columnList = "resident_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshOrder {

    public enum OrderStatus { PENDING, CONFIRMED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * The ordered lines. EAGER because the controller serialises the entity directly
     * and a LAZY collection would throw once the transaction has closed. Orders carry
     * a handful of lines, so this is cheap; if "my orders" ever grows large enough for
     * the per-order select to matter, switch to a fetch-join query rather than to LAZY.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @Builder.Default
    private java.util.List<FreshOrderItem> items = new java.util.ArrayList<>();

    /** Keeps both sides of the association consistent before a cascade save. */
    public void addItem(FreshOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_slot", length = 100)
    private String deliverySlot;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt    == null) createdAt    = now;
        if (updatedAt    == null) updatedAt    = now;
        if (status       == null) status       = OrderStatus.PENDING;
        if (totalAmount  == null) totalAmount  = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
