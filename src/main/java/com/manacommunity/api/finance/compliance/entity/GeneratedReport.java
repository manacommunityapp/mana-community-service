package com.manacommunity.api.finance.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "generated_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "report_period", nullable = false, length = 30)
    private String reportPeriod;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    @Builder.Default
    private String fileFormat = "PDF";

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "jsonb")
    private String metadata;
}
