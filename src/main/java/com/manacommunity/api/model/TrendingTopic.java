package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trending_topic", indexes = {
    @Index(name = "idx_trending_community", columnList = "community_id"),
    @Index(name = "idx_trending_score", columnList = "score DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "topic_type", nullable = false, length = 30)
    @Builder.Default
    private String topicType = "HASHTAG";

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private int postCount = 0;

    @Column(name = "engagement_count", nullable = false)
    @Builder.Default
    private int engagementCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private double score = 0.0;

    @Column(name = "time_window_hours", nullable = false)
    @Builder.Default
    private int timeWindowHours = 24;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
