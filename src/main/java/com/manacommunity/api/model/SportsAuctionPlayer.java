package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "sports_auction_player")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class SportsAuctionPlayer {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SportsAuctionConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String category;   // BATSMEN | BOWLERS | ALL_ROUNDERS

    private String playerRole;
    private Integer age;

    @Column(nullable = false)
    private Integer basePrice;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String statsJson;  // {"matches":42,"runs":1200,"wickets":38}

    @Column(nullable = false)
    private Integer queueOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStatus status;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private SportsAuctionTeam assignedTeam;

    private Long soldPrice;
    private Boolean rtmUsed;
    private LocalDateTime soldAt;
    private LocalDateTime uploadedAt;

    public enum PlayerStatus { QUEUED, SELLING, SOLD, PASSED, RETAINED }
}
