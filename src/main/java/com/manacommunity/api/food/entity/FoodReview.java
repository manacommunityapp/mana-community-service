package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 30)
    private ReviewEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column
    private Integer rating;

    @Column(length = 200)
    private String title;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(columnDefinition = "TEXT")
    private String images;

    @Builder.Default
    @Column(name = "helpful_count")
    private Integer helpfulCount = 0;

    @Builder.Default
    @Column
    private Boolean reported = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReviewEntityType {
        RESTAURANT, HOME_CHEF, RECIPE, GROCERY_STORE, CATERER, PRODUCT
    }

    public enum ReviewStatus {
        PUBLISHED, HIDDEN, REMOVED
    }
}
