package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_subscription_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSubscriptionDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private FoodSubscription subscription;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "meal_type", length = 50)
    private String mealType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeliveryStatus status;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_partner_id")
    private Long deliveryPartnerId;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum DeliveryStatus { SCHEDULED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, SKIPPED, CANCELLED }
}
