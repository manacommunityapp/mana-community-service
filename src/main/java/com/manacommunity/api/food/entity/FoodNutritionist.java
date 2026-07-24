package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_nutritionists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(length = 200)
    private String qualification;

    @Column(length = 200)
    private String specialization;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NutritionistStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum NutritionistStatus { ACTIVE, INACTIVE, SUSPENDED }
}
