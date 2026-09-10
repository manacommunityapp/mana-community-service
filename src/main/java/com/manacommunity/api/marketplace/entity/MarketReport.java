package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_reports", indexes = {
    @Index(name = "idx_mkt_rep_listing", columnList = "listing_id"),
    @Index(name = "idx_mkt_rep_status", columnList = "status"),
    @Index(name = "idx_mkt_rep_comm", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private MarketListing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "action_taken", length = 500)
    private String actionTaken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_id")
    private AppUser moderatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ReportStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReportStatus { PENDING, REVIEWED, DISMISSED, ACTION_TAKEN }
}
