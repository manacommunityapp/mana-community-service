package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per device per user. A user can have multiple active tokens
 * (phone + tablet, or after reinstall before logout clears the old one).
 *
 * token format: "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"
 * or for managed devices: "ExponentPushToken[xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx]"
 */
@Entity
@Table(
    name = "push_token",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_push_token_value",
        columnNames = "token"               // one row per physical token value
    ),
    indexes = {
        @Index(name = "idx_push_token_user", columnList = "user_id"),
        @Index(name = "idx_push_token_value", columnList = "token")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Expo push token string, e.g. "ExponentPushToken[AAABBB...]" */
    @Column(nullable = false, length = 200)
    private String token;

    /** "ios" or "android" */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String platform = "android";

    /**
     * Set to false when Expo returns DeviceNotRegistered for this token.
     * We keep the row for audit instead of hard-deleting it.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
