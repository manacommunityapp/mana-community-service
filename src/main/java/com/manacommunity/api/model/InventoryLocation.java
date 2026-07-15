package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_location", indexes = {
    @Index(name = "idx_inv_location_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_inv_location_usage",     columnList = "usage"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "warehouse")
public class InventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Full hierarchical path, e.g. "WH/Stock", "Partner/Vendor". */
    @Column(name = "complete_name", nullable = false, length = 200)
    private String completeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LocationUsage usage = LocationUsage.INTERNAL;

    @Column(length = 100)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    @JsonIgnore
    private InventoryWarehouse warehouse;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum LocationUsage {
        VIEW, INTERNAL, CUSTOMER, VENDOR, INVENTORY, PRODUCTION, TRANSIT, SCRAP
    }
}
