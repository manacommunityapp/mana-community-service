package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "borrowed_by", nullable = false, length = 120)
    private String borrowedBy;

    @Column(name = "borrowed_by_flat", nullable = false, length = 30)
    private String borrowedByFlat;

    @Column(name = "borrowed_at", nullable = false)
    private LocalDateTime borrowedAt;

    @Column(name = "expected_return_at", nullable = false, length = 60)
    private String expectedReturnAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
}
