package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "food_home_chef_menus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodHomeChefMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", nullable = false)
    private FoodHomeChef chef;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 100)
    private String category;

    @Column(name = "is_veg")
    private Boolean isVeg;

    private Integer calories;

    @Column(precision = 10, scale = 2)
    private BigDecimal protein;

    @Column(name = "preparation_time")
    private Integer preparationTime;

    @Column(name = "available_days", columnDefinition = "TEXT")
    private String availableDays;

    @Column(name = "order_before_time")
    private LocalTime orderBeforeTime;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
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
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
