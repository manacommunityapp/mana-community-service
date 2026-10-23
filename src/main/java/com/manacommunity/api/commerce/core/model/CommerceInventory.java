package com.manacommunity.api.commerce.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private CommerceProduct product;

    @Column(name = "available_stock", nullable = false)
    @Builder.Default
    private int availableStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private int reservedStock = 0;

    @Column(name = "committed_stock", nullable = false)
    @Builder.Default
    private int committedStock = 0;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "reorder_point")
    @Builder.Default
    private int reorderPoint = 5;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
