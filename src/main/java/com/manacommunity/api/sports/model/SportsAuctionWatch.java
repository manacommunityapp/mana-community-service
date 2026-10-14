package com.manacommunity.api.sports.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_auction_watch",
       uniqueConstraints = @UniqueConstraint(name = "uq_auction_watch_user_config", columnNames = {"user_id", "config_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsAuctionWatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private SportsAuctionConfig config;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
