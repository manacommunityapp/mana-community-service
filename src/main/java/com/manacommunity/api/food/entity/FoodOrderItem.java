package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private FoodOrder order;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "variant_name", length = 200)
    private String variantName;

    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "is_veg")
    private Boolean isVeg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
