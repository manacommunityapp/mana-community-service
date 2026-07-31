package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_subscription_pauses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSubscriptionPause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private FoodSubscription subscription;

    @Column(name = "pause_start")
    private LocalDate pauseStart;

    @Column(name = "pause_end")
    private LocalDate pauseEnd;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PauseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum PauseStatus { ACTIVE, COMPLETED, CANCELLED }
}
