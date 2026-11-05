package com.manacommunity.api.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_program_enrollment", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgramEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "current_week", nullable = false)
    private Integer currentWeek;

    @Column(name = "current_day", nullable = false)
    private Integer currentDay;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "enrolled_at")
    private OffsetDateTime enrolledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    void prePersist() {
        enrolledAt = OffsetDateTime.now();
    }
}
