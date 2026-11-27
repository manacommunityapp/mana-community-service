package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_treatment_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterTreatmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "plant_type", nullable = false, length = 30)
    private String plantType; // WTP, STP

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType; // MGF_BACKWASH, ACF_BACKWASH, SOFTENER_SALT_RECHARGE, STP_AERATION_CHECK, CHLORINE_DOSING, SLUDGE_DISPOSAL

    @Column(name = "log_date", nullable = false)
    @Builder.Default
    private LocalDateTime logDate = LocalDateTime.now();

    @Column(name = "quantity_used_kg", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal quantityUsedKg = BigDecimal.ZERO;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
