package com.manacommunity.api.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trainer_program_day", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerProgramDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "day_name")
    private String dayName;

    @Column(name = "is_rest_day", nullable = false)
    private Boolean isRestDay;

    @Column(name = "focus")
    private String focus;

    @Column(columnDefinition = "TEXT")
    private String exercises;

    @Column(name = "total_duration_min")
    private Integer totalDurationMin;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
