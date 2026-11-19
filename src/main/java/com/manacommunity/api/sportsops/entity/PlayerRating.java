package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "player_rating",
    uniqueConstraints = @UniqueConstraint(columnNames = {"community_id","rated_user_id","rated_by","sport","session_id"}))
@Data
public class PlayerRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "rated_user_id", nullable = false) private Long ratedUserId;
    @Column(name = "rated_by", nullable = false) private Long ratedBy;
    @Column(nullable = false, length = 100) private String sport;
    @Column(name = "session_id") private Long sessionId;
    @Column(nullable = false) private Integer rating;
    @Column private Integer sportsmanship;
    @Column(name = "skill_level") private Integer skillLevel;
    @Column(length = 500) private String comment;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
