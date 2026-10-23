package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Additional per-community configuration not stored in the base Community
 * entity (which has minimal fields in the original schema).
 *
 * Relationship: one CommunitySettings row per Community (created lazily on
 * first admin settings save; GET returns defaults if row is absent).
 */
@Entity
@Table(name = "community_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunitySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false, unique = true)
    private Community community;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String address;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 500;

    // ── Feature toggles ──────────────────────────────────────────────
    @Column(name = "feature_marketplace")
    @Builder.Default
    private Boolean featureMarketplace = true;

    @Column(name = "feature_sports")
    @Builder.Default
    private Boolean featureSports = true;

    @Column(name = "feature_auction")
    @Builder.Default
    private Boolean featureAuction = true;

    @Column(name = "feature_jobs")
    @Builder.Default
    private Boolean featureJobs = true;

    @Column(name = "feature_polls")
    @Builder.Default
    private Boolean featurePolls = true;
}
