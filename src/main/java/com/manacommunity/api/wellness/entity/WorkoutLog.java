package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "workout_log", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_catalog_id")
    private Long exerciseCatalogId;

    @Column(name = "exercise_name", nullable = false)
    private String exerciseName;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(name = "sets")
    private Integer sets;

    @Column(name = "reps_per_set")
    private Integer repsPerSet;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "notes")
    private String notes;
}
