package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_engagement_score", uniqueConstraints = {
    @UniqueConstraint(name = "uk_engagement_user_community", columnNames = {"user_id", "community_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEngagementScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private int totalPoints = 0;

    @Column(name = "posts_count", nullable = false)
    @Builder.Default
    private int postsCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private int commentsCount = 0;

    @Column(name = "reactions_given", nullable = false)
    @Builder.Default
    private int reactionsGiven = 0;

    @Column(name = "reactions_received", nullable = false)
    @Builder.Default
    private int reactionsReceived = 0;

    @Column(name = "groups_joined", nullable = false)
    @Builder.Default
    private int groupsJoined = 0;

    @Column(name = "events_attended", nullable = false)
    @Builder.Default
    private int eventsAttended = 0;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private int helpfulCount = 0;

    @Column(name = "volunteer_points", nullable = false)
    @Builder.Default
    private int volunteerPoints = 0;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private int longestStreak = 0;

    @Column(name = "level", nullable = false)
    @Builder.Default
    private int level = 1;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
