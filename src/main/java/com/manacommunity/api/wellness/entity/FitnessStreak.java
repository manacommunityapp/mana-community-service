package com.manacommunity.api.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fitness_streak", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_streak_days")
    private Integer currentStreakDays;

    @Column(name = "longest_streak_days")
    private Integer longestStreakDays;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_active_days")
    private Integer totalActiveDays;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
