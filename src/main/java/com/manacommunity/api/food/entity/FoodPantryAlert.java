package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_pantry_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPantryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pantry_item_id", nullable = false)
    private FoodPantryItem pantryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 20)
    private AlertType alertType;

    @Column(length = 500)
    private String message;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum AlertType { EXPIRING_SOON, EXPIRED, LOW_STOCK, REORDER }
}
