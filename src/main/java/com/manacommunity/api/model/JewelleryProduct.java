package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "jewellery_product", indexes = {
    @Index(name = "idx_jp_vendor",   columnList = "vendor_id"),
    @Index(name = "idx_jp_category", columnList = "category, status"),
    @Index(name = "idx_jp_status",   columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JewelleryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private JewelleryVendorProfile vendor;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "jewellery_type", length = 60)
    private String jewelleryType;

    @Column(name = "material", nullable = false, length = 40)
    private String material;

    @Column(name = "purity", length = 30)
    private String purity;

    @Column(name = "net_weight_g", precision = 10, scale = 3)
    private BigDecimal netWeightG;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "huid", length = 16)
    private String huid;

    @Column(name = "tags", length = 300)
    private String tags;

    @Column(name = "gold_rate_per_gram", precision = 10, scale = 2)
    private BigDecimal goldRatePerGram;

    @Column(name = "making_charges", precision = 10, scale = 2)
    private BigDecimal makingCharges;

    @Column(name = "stone_cost", precision = 10, scale = 2)
    private BigDecimal stoneCost;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "gst_percent", precision = 4, scale = 2)
    private BigDecimal gstPercent;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stone_details", columnDefinition = "jsonb")
    private Map<String, Object> stoneDetails;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

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
