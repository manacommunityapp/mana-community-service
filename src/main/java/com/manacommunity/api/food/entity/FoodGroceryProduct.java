package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_grocery_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodGroceryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private FoodGroceryStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FoodGroceryCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(length = 200)
    private String brand;

    @Column(length = 50)
    private String unit;

    @Column(name = "unit_value", precision = 10, scale = 2)
    private BigDecimal unitValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    private Integer stock;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @Column(length = 100)
    private String barcode;

    @Column(name = "is_organic", nullable = false)
    @Builder.Default
    private Boolean isOrganic = false;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "nutritional_info", columnDefinition = "TEXT")
    private String nutritionalInfo;

    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

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
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
