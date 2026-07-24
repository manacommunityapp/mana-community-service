package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_kitchen_waste_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodKitchenWasteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kitchen_type", length = 50)
    private String kitchenType;

    @Column(name = "kitchen_id")
    private Long kitchenId;

    @Column
    private LocalDate date;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WasteReason reason;

    @Column(name = "cost_impact", precision = 10, scale = 2)
    private BigDecimal costImpact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_id")
    private AppUser loggedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum WasteReason { EXPIRED, SPOILED, OVERPRODUCTION, DAMAGED }
}
