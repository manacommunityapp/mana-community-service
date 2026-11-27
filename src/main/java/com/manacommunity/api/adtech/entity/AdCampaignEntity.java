package com.manacommunity.api.adtech.entity;

import com.manacommunity.api.adtech.dto.AdTechDtos.CampaignTargetingDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_campaigns", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class AdCampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false, unique = true)
    private UUID campaignId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 0L;

    @Column(name = "brand_id", nullable = false, length = 100)
    private String brandId;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "total_budget_paise", nullable = false)
    private long totalBudgetPaise;

    @Column(name = "spent_paise", nullable = false)
    private long spentPaise = 0L;

    @Column(name = "cpm_paise", nullable = false)
    private long cpmPaise;

    @Column(name = "daily_cap_paise")
    private Long dailyCapPaise;

    @Column(name = "freq_cap_per_day", nullable = false)
    private int freqCapPerDay = 3;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "targeting", columnDefinition = "jsonb", nullable = false)
    private CampaignTargetingDto targeting;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy = 0L;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;
}
