package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_loyalty_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceLoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "total_points_earned", nullable = false)
    @Builder.Default
    private Long totalPointsEarned = 0L;

    @Column(name = "total_points_spent", nullable = false)
    @Builder.Default
    private Long totalPointsSpent = 0L;

    @Column(name = "available_points", nullable = false)
    @Builder.Default
    private Long availablePoints = 0L;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tier = "BRONZE";

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void addPoints(long pts) {
        availablePoints += pts;
        totalPointsEarned += pts;
        recalcTier();
    }

    public void deductPoints(long pts) {
        if (availablePoints < pts) throw new IllegalStateException("Insufficient loyalty points");
        availablePoints -= pts;
        totalPointsSpent += pts;
    }

    private void recalcTier() {
        if (totalPointsEarned >= 10000) tier = "PLATINUM";
        else if (totalPointsEarned >= 5000) tier = "GOLD";
        else if (totalPointsEarned >= 1000) tier = "SILVER";
        else tier = "BRONZE";
    }
}
