package com.manacommunity.api.guard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guard_daily_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardDailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "guard_user_id", nullable = false)
    private Long guardUserId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "shift_type", length = 20, nullable = false)
    @Builder.Default
    private String shiftType = "MORNING";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String entry;

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
