package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "court_slot_subscription")
@Data
public class CourtSlotSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "court_id", nullable = false) private Long courtId;
    @Column(name = "day_of_week", nullable = false) private Integer dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(length = 20) private String frequency = "WEEKLY";
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column private Boolean active = true;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
