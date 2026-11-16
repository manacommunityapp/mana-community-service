package com.manacommunity.api.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_goal", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "daily_steps_target")
    private Integer dailyStepsTarget;

    @Column(name = "active_minutes_target")
    private Integer activeMinutesTarget;

    @Column(name = "weekly_workouts_target")
    private Integer weeklyWorkoutsTarget;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
