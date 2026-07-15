package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_picking", indexes = {
    @Index(name = "idx_inv_picking_type",      columnList = "picking_type_id"),
    @Index(name = "idx_inv_picking_state",     columnList = "state"),
    @Index(name = "idx_inv_picking_community", columnList = "community_id"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"pickingType", "location", "locationDest", "moveLines", "backorder", "community", "createdByUser"})
public class InventoryPicking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Auto-generated name, e.g. "WH/IN/00001". */
    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PickingState state = PickingState.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_type_id", nullable = false)
    private InventoryPickingType pickingType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_dest_id", nullable = false)
    private InventoryLocation locationDest;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "date_done")
    private LocalDateTime dateDone;

    /** Source document reference, e.g. "PO-0042", "Manual Inventory Adjustment". */
    @Column(length = 200)
    private String origin;

    /** If this picking was created as a backorder from a partial validation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backorder_id")
    private InventoryPicking backorder;

    @OneToMany(mappedBy = "picking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryMoveLine> moveLines = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum PickingState {
        DRAFT, WAITING, CONFIRMED, ASSIGNED, DONE, CANCEL
    }
}
