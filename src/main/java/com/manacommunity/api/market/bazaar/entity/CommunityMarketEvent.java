package com.manacommunity.api.market.bazaar.entity;

import com.manacommunity.api.events.entity.EventCommunity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Bazaar configuration is keyed by the canonical EventCommunity id. */
@Entity
@Table(name = "market_event_bazaar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityMarketEvent {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id", nullable = false)
    private EventCommunity event;

    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "banner_url", length = 1000)
    private String bannerUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

