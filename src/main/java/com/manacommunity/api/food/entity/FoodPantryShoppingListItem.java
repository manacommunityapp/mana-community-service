package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_pantry_shopping_list_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPantryShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private FoodPantryShoppingList list;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(length = 100)
    private String category;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "estimated_price", precision = 10, scale = 2)
    private BigDecimal estimatedPrice;

    @Builder.Default
    @Column(name = "is_purchased")
    private Boolean isPurchased = false;

    @Column(name = "purchased_price", precision = 10, scale = 2)
    private BigDecimal purchasedPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
