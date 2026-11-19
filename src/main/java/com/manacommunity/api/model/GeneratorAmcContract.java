package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generator_amc_contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorAmcContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "generator_id", nullable = false)
    private GeneratorAsset generator;

    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @Column(name = "contract_number", nullable = false, length = 80)
    private String contractNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "annual_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualCost;

    @Column(name = "visits_included", nullable = false)
    @Builder.Default
    private Integer visitsIncluded = 4;

    @Column(name = "visits_completed", nullable = false)
    @Builder.Default
    private Integer visitsCompleted = 0;

    @Column(name = "emergency_helpline", length = 100)
    private String emergencyHelpline;

    @Column(name = "sla_hours")
    @Builder.Default
    private Integer slaHours = 4;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRING_SOON, EXPIRED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (visitsIncluded == null) visitsIncluded = 4;
        if (visitsCompleted == null) visitsCompleted = 0;
        if (slaHours == null) slaHours = 4;
        if (status == null) status = "ACTIVE";
    }
}
