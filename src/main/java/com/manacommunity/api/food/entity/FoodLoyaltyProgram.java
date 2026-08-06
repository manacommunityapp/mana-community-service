package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_loyalty_programs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLoyaltyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", length = 20)
    private ProgramType programType;

    @Column(name = "points_per_rupee", precision = 10, scale = 2)
    private BigDecimal pointsPerRupee;

    @Column(name = "min_redeem_points")
    private Integer minRedeemPoints;

    @Column(name = "point_value", precision = 10, scale = 4)
    private BigDecimal pointValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProgramStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ProgramType { POINTS, CASHBACK, TIER }

    public enum ProgramStatus { ACTIVE, INACTIVE }
}
