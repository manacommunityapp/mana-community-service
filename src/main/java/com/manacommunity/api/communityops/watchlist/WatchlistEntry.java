package com.manacommunity.api.communityops.watchlist;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity(name = "CommunityOpsWatchlistEntry")
@Table(name = "watchlist_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchlistType type;

    private String name;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchlistSeverity severity = WatchlistSeverity.CAUTION;

    @Column(name = "added_by", nullable = false)
    private Long addedBy;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
