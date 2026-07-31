package com.manacommunity.api.food.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_pantry_consumption_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPantryConsumptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pantry_item_id", nullable = false)
    private FoodPantryItem pantryItem;

    @Column(name = "quantity_used", precision = 10, scale = 2)
    private BigDecimal quantityUsed;

    @Column(name = "used_for", length = 200)
    private String usedFor;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
