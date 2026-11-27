package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_resource_quota", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tenant_quota",
                      columnNames = {"community_id", "resource_type"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResourceQuota extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    private Community community;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "max_concurrent_queries")
    @Builder.Default
    private Integer maxConcurrentQueries = 5;

    @Column(name = "max_rows_per_export")
    @Builder.Default
    private Integer maxRowsPerExport = 50000;

    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;

    @Column(name = "priority_weight")
    @Builder.Default
    private Integer priorityWeight = 10;

    @Column(name = "burst_tokens")
    @Builder.Default
    private Integer burstTokens = 20;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
