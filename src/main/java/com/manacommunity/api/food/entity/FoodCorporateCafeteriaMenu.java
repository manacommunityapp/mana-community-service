package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_corporate_cafeteria_menus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCorporateCafeteriaMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id", nullable = false)
    private FoodCorporateCafeteria cafeteria;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "meal_type", length = 50)
    private String mealType;

    @Column(columnDefinition = "TEXT")
    private String items;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_plates")
    private Integer totalPlates;

    @Column(name = "booked_plates")
    @Builder.Default
    private Integer bookedPlates = 0;

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
