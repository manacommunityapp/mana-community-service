package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_order_item_addons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderItemAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private FoodOrderItem orderItem;

    @Column(name = "addon_name", length = 200)
    private String addonName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
