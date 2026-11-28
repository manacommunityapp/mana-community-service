package com.manacommunity.api.finops.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storage_tier_policies", schema = "finops")
@Getter
@Setter
@NoArgsConstructor
public class StorageTierPolicyEntity {

    @Id
    @Column(name = "asset_class", nullable = false, length = 50)
    private String assetClass;

    @Column(name = "target_format", nullable = false, length = 20)
    private String targetFormat;

    @Column(name = "hot_tier", nullable = false, length = 40)
    private String hotTier;

    @Column(name = "hot_days", nullable = false)
    private int hotDays;

    @Column(name = "warm_tier", nullable = false, length = 40)
    private String warmTier;

    @Column(name = "warm_days", nullable = false)
    private int warmDays;

    @Column(name = "archive_tier", nullable = false, length = 40)
    private String archiveTier;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "min_billable_kb", nullable = false)
    private int minBillableKb = 128;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
