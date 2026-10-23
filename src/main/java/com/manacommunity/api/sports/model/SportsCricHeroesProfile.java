package com.manacommunity.api.sports.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_cricheroes_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsCricHeroesProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private SportsAuctionPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "cricheroes_id", nullable = false, length = 100)
    private String cricheroesId;

    @Column(name = "share_url", nullable = false, length = 500)
    private String shareUrl;

    @Column(name = "resolved_url", length = 500)
    private String resolvedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "format_scope", nullable = false, length = 20)
    @Builder.Default
    private FormatScope formatScope = FormatScope.OVERALL;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "bio_json", columnDefinition = "jsonb")
    private String bioJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "batting_json", columnDefinition = "jsonb")
    private String battingJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "bowling_json", columnDefinition = "jsonb")
    private String bowlingJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "fielding_json", columnDefinition = "jsonb")
    private String fieldingJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "recent_form_json", columnDefinition = "jsonb")
    private String recentFormJson;

    @Column(name = "mvp_points")
    private Integer mvpPoints;

    @Column(name = "rating_overall")
    private Double ratingOverall;

    @Column(name = "rating_tier", length = 20)
    private String ratingTier;

    @Column(name = "rating_badges", length = 500)
    private String ratingBadges;

    @Column(name = "suggested_base_price")
    private Integer suggestedBasePrice;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (verifiedAt == null) verifiedAt = now;
        if (syncedAt == null) syncedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FormatScope { TENNIS, LEATHER, OVERALL }
}
