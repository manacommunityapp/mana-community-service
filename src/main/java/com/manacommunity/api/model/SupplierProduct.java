package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierProfile supplier;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 800)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProcurementCategory category;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "retail_price", precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer moq = 1;

    @Column(name = "max_order_qty")
    private Integer maxOrderQty;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.valueOf(18);

    @Column(name = "lead_time_days", nullable = false)
    @Builder.Default
    private Integer leadTimeDays = 3;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 1000)
    private String specifications;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
