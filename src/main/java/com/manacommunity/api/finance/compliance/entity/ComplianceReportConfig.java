package com.manacommunity.api.finance.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "compliance_report_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "report_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String frequency = "MONTHLY";

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @Column(columnDefinition = "jsonb")
    private String recipients;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
