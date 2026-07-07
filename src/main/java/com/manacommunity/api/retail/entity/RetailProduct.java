package com.manacommunity.api.retail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "retail_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 10)
    private String emoji;

    @Column(length = 60)
    private String category;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;

    @Column(name = "units_ordered", nullable = false)
    private Integer unitsOrdered;

    @Column(name = "units_sold", nullable = false)
    private Integer unitsSold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private com.manacommunity.api.model.Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (reorderLevel == null) reorderLevel = 10;
        if (unitsOrdered == null) unitsOrdered = 0;
        if (unitsSold == null) unitsSold = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
