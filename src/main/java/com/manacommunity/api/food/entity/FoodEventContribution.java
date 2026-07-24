package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_event_contributions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodEventContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private FoodEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20)
    private ContributionType itemType;

    @Column(length = 100)
    private String quantity;

    @Column(name = "serving_size", length = 100)
    private String servingSize;

    @Column(name = "is_veg")
    private Boolean isVeg;

    @Column(columnDefinition = "TEXT")
    private String allergens;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ContributionType { FOOD, BEVERAGE, DESSERT, EQUIPMENT }
}
