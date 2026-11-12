package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_fresh_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwFreshAssignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operational_assignment_id")
    private MwOperationalAssignment operationalAssignment;

    @Column(length = 100)
    private String freshOrderId;

    @Column(length = 300)
    private String pickupLocation;

    @Column(length = 300)
    private String dropLocation;

    private BigDecimal estimatedKm;
    private BigDecimal actualKm;
    private Boolean onTime;
    private BigDecimal basePay;
    private BigDecimal kmBonus;
    private BigDecimal timeBonus;

    @Column(name = "total_incentive", insertable = false, updatable = false)
    private BigDecimal totalIncentive;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (kmBonus == null) kmBonus = BigDecimal.ZERO;
        if (timeBonus == null) timeBonus = BigDecimal.ZERO;
    }
}
