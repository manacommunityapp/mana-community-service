package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_resident_favorites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResidentFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FoodResidentProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "favorite_type", nullable = false, length = 20)
    private FavoriteType favoriteType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum FavoriteType { RESTAURANT, HOME_CHEF, RECIPE, DISH }
}
