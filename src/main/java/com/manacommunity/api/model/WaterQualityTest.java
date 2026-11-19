package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_quality_tests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterQualityTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "test_date", nullable = false)
    @Builder.Default
    private LocalDateTime testDate = LocalDateTime.now();

    @Column(name = "source_location", nullable = false, length = 80)
    private String sourceLocation; // MUNICIPAL_INFLOW, BOREWELL_1, WTP_OUTLET, STP_TREATED, TOWER_A_TAP

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal ph;

    @Column(name = "tds_ppm", nullable = false)
    private Integer tdsPpm;

    @Column(name = "hardness_ppm", nullable = false)
    private Integer hardnessPpm;

    @Column(name = "residual_chlorine_ppm", precision = 4, scale = 2)
    private BigDecimal residualChlorinePpm;

    @Column(name = "turbidity_ntu", precision = 4, scale = 2)
    private BigDecimal turbidityNtu;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SAFE"; // SAFE, MARGINAL, CONTAMINATED

    @Column(name = "tested_by", length = 120)
    private String testedBy;

    @Column(name = "lab_report_url")
    private String labReportUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
