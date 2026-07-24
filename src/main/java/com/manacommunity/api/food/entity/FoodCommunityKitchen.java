package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "food_community_kitchens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCommunityKitchen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kitchen_type", nullable = false, length = 30)
    private CommunityKitchenType kitchenType;

    @Column(length = 500)
    private String location;

    @Column(length = 1000)
    private String description;

    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private AppUser manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommunityKitchenStatus status = CommunityKitchenStatus.ACTIVE;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

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

    public enum CommunityKitchenType { CLUBHOUSE, CAFETERIA, TEMPLE, CORPORATE, SENIOR_CITIZEN }
    public enum CommunityKitchenStatus { ACTIVE, INACTIVE, MAINTENANCE }
}
