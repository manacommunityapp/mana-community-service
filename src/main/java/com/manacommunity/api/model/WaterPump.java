package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_pumps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterPump {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "pump_type", nullable = false, length = 50)
    private String pumpType; // TRANSFER_PUMP, BOOSTER_PUMP, BOREWELL_PUMP, DRAINAGE_PUMP, FIRE_HYDRANT

    @Column(length = 150)
    private String location;

    @Column(name = "rated_kw", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal ratedKw = BigDecimal.valueOf(7.5);

    @Column(name = "duty_cycle_hours", nullable = false)
    @Builder.Default
    private Integer dutyCycleHours = 4;

    @Column(name = "total_run_hours", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalRunHours = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "STANDBY"; // RUNNING, STANDBY, MAINTENANCE, FAULT

    @Column(name = "is_duty_pump", nullable = false)
    @Builder.Default
    private Boolean isDutyPump = false;

    @Column(name = "motor_health_rating", length = 30)
    @Builder.Default
    private String motorHealthRating = "GOOD"; // EXCELLENT, GOOD, ATTENTION_NEEDED, CRITICAL

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
