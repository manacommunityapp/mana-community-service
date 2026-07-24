package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "notification_type", length = 100)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationChannel channel;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationChannel {
        PUSH, EMAIL, SMS, IN_APP
    }

    public enum NotificationStatus {
        SENT, DELIVERED, READ, FAILED
    }
}
