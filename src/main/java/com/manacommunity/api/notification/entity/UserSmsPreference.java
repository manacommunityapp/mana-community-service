package com.manacommunity.api.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sms_preference", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_sms_pref_user_type",
                columnNames = {"user_id", "notification_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSmsPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Notification type key, e.g. "EVENT_REMINDER", "PAYMENT_RECEIPT",
     * "COMMUNITY_ALERT". Must match a value the front-end understands.
     */
    @Column(name = "notification_type", nullable = false, length = 80)
    private String notificationType;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = true;

    @Column(name = "whatsapp_enabled")
    @Builder.Default
    private boolean whatsappEnabled = false;

    /** Preferred language for this notification type */
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "EN";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
