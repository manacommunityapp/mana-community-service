package com.manacommunity.api.wellnessbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Per-resident wellness subscription preferences.
 * One active row per subscribing resident.
 */
@Getter
@Setter
@Entity
@Table(name = "wellness_subscriber_preference", schema = "manacommunity")
public class WellnessSubscriberPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References app_user.id */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** BASIC | PREMIUM */
    @Column(name = "plan_type", nullable = false, length = 30)
    private String planType = "BASIC";

    /** SMALL | STANDARD | LARGE */
    @Column(name = "portion_size", nullable = false, length = 20)
    private String portionSize = "STANDARD";

    /** Maps to WellnessProfile.goalType values */
    @Column(name = "goal_type", length = 40)
    private String goalType;

    /** Ingredient names the resident dislikes (excluded from menu) */
    @Column(name = "disliked_ingredients", columnDefinition = "text[]")
    private String[] dislikedIngredients = new String[0];

    @Column(name = "extra_notes", columnDefinition = "text")
    private String extraNotes;

    /** ACTIVE | PAUSED | CANCELLED */
    @Column(name = "subscription_status", nullable = false, length = 20)
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "pause_from")
    private LocalDate pauseFrom;

    @Column(name = "pause_until")
    private LocalDate pauseUntil;

    /** Cashfree recurring mandate ID */
    @Column(name = "cashfree_subscription_id", length = 150)
    private String cashfreeSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
    private WellnessCommunityCluster cluster;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
