package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_catering_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCateringPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caterer_id", nullable = false)
    private FoodCaterer caterer;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "occasion_type", length = 30)
    private OccasionType occasionType;

    @Column(name = "items_per_plate")
    private Integer itemsPerPlate;

    @Column(name = "price_per_plate", precision = 10, scale = 2)
    private BigDecimal pricePerPlate;

    @Column(name = "min_plates")
    private Integer minPlates;

    @Column(columnDefinition = "TEXT")
    private String includes;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

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

    public enum OccasionType { BIRTHDAY, WEDDING, HOUSEWARMING, CORPORATE, FESTIVAL, SPORTS, SCHOOL }
}
