package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "open_play_session")
@Data
public class OpenPlaySession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(nullable = false, length = 100) private String sport;
    @Column(name = "court_id", nullable = false) private Long courtId;
    @Column(name = "session_date", nullable = false) private LocalDate sessionDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "min_players") private Integer minPlayers = 2;
    @Column(name = "max_players", nullable = false) private Integer maxPlayers;
    @Column(name = "skill_level", length = 50) private String skillLevel;
    @Column(name = "organizer_id", nullable = false) private Long organizerId;
    @Column(length = 50) private String status = "OPEN";
    @Column(length = 500) private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
