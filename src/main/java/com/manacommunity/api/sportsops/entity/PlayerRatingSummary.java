package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "player_rating_summary", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "sport"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerRatingSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String sport;
    @Column(name = "games_played") private Integer gamesPlayed = 0;
    @Column(name = "avg_rating", precision = 3, scale = 2) private BigDecimal avgRating = BigDecimal.ZERO;
    @Column(name = "avg_sportsmanship", precision = 3, scale = 2) private BigDecimal avgSportsmanship = BigDecimal.ZERO;
    @Column(name = "total_ratings") private Integer totalRatings = 0;
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
