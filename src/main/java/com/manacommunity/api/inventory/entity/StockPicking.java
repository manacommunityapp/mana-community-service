package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_picking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPicking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(length = 80)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PickingState state;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_dest_id")
    private Long locationDestId;

    @Column(name = "picking_type_id")
    private Long pickingTypeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (state == null) {
            state = PickingState.draft;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
