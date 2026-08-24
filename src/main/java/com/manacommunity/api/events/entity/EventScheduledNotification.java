package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_scheduled_notification", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventScheduledNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityEvent event;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String type = "reminder";

    @Column(length = 200)
    private String channels;

    @Column(name = "send_now")
    @Builder.Default
    private boolean sendNow = false;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(length = 50)
    @Builder.Default
    private String repeatMode = "none";

    @Column(name = "custom_repeat_days")
    private Integer customRepeatDays;

    @Column(name = "send_to_all")
    @Builder.Default
    private boolean sendToAll = true;

    @Column(columnDefinition = "TEXT")
    private String recipientIdsJson;

    @Column(columnDefinition = "TEXT")
    private String customMessage;

    @Column(length = 30)
    @Builder.Default
    private String status = "scheduled";

    @Column(name = "recipients_count")
    @Builder.Default
    private int recipientsCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
