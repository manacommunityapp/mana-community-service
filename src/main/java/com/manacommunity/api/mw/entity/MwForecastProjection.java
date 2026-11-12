package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_forecast_projections")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwForecastProjection {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private LocalDate projectedWeek;
    private String sourceModule;
    private Integer projectedDemand;
    private Integer confidencePct;
    private LocalDateTime generatedAt;

    @PrePersist
    void onCreate() {
        generatedAt = LocalDateTime.now();
        if (confidencePct == null) confidencePct = 70;
    }
}
