package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_picking_type", indexes = {
    @Index(name = "idx_inv_pt_warehouse", columnList = "warehouse_id"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"warehouse", "defaultLocationSrc", "defaultLocationDest"})
public class InventoryPickingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickingTypeCode code;

    /** Prefix for auto-generated picking names, e.g. "WH/IN/". */
    @Column(name = "sequence_prefix", nullable = false, length = 20)
    private String sequencePrefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private InventoryWarehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_location_src_id")
    private InventoryLocation defaultLocationSrc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_location_dest_id")
    private InventoryLocation defaultLocationDest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum PickingTypeCode { INCOMING, OUTGOING, INTERNAL }
}
