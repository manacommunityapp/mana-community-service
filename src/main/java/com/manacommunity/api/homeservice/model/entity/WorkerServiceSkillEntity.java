package com.manacommunity.api.homeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_service_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerServiceSkillEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "category_id", nullable = false, length = 64)
    private String categoryId;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(name = "base_price_monthly", precision = 10, scale = 2)
    private BigDecimal basePriceMonthly;

    @Column(name = "base_price_per_day", precision = 10, scale = 2)
    private BigDecimal basePricePerDay;

    @Column(name = "base_price_per_visit", precision = 10, scale = 2)
    private BigDecimal basePricePerVisit;

    @Column(name = "base_price_per_hour", precision = 10, scale = 2)
    private BigDecimal basePricePerHour;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
