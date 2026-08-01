package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_group_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodGroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GroupOrderStatus status;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "join_code", length = 20)
    private String joinCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", length = 20)
    private SplitType splitType;

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

    public enum GroupOrderStatus { OPEN, LOCKED, ORDERED, COMPLETED, CANCELLED }

    public enum SplitType { EQUAL, INDIVIDUAL }
}
