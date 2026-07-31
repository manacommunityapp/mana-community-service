package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Builder.Default
    @Column(name = "order_updates")
    private Boolean orderUpdates = true;

    @Builder.Default
    @Column
    private Boolean promotions = true;

    @Builder.Default
    @Column(name = "subscription_reminders")
    private Boolean subscriptionReminders = true;

    @Builder.Default
    @Column(name = "expiry_alerts")
    private Boolean expiryAlerts = true;

    @Builder.Default
    @Column(name = "event_notifications")
    private Boolean eventNotifications = true;

    @Builder.Default
    @Column(name = "nutrition_reminders")
    private Boolean nutritionReminders = true;

    @Builder.Default
    @Column(name = "delivery_updates")
    private Boolean deliveryUpdates = true;

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
}
