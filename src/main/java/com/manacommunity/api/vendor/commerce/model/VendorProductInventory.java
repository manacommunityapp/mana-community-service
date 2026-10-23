package com.manacommunity.api.vendor.commerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_product_inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProductInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    @JsonIgnore
    private VendorProductVariant variant;

    @Column(name = "available_qty", nullable = false)
    @Builder.Default
    private Integer availableQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    @Builder.Default
    private Integer reservedQty = 0;

    @Column(name = "committed_qty", nullable = false)
    @Builder.Default
    private Integer committedQty = 0;

    @Column(name = "allocated_qty", nullable = false)
    @Builder.Default
    private Integer allocatedQty = 0;

    @Column(name = "picked_qty", nullable = false)
    @Builder.Default
    private Integer pickedQty = 0;

    @Column(name = "dispatched_qty", nullable = false)
    @Builder.Default
    private Integer dispatchedQty = 0;

    @Column(name = "delivered_qty", nullable = false)
    @Builder.Default
    private Integer deliveredQty = 0;

    @Column(name = "damaged_qty", nullable = false)
    @Builder.Default
    private Integer damagedQty = 0;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
