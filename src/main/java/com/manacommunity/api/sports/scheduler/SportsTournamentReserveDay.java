package com.manacommunity.api.sports.scheduler;

import com.manacommunity.api.sports.model.SportsVenue;
import com.manacommunity.api.sports.model.SportsCourt;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sports_tournament_reserve_day")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsTournamentReserveDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private SportsTournamentConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private SportsTournamentStage stage;

    @Column(nullable = false)
    private LocalDate reserveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private SportsVenue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private SportsCourt court;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReserveDayType reserveType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReserveDayStatus status = ReserveDayStatus.AVAILABLE;

    private LocalDateTime activatedAt;
    private String activatedBy;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ReserveDayType {
        TOURNAMENT_WIDE, STAGE_SPECIFIC, MATCH_SPECIFIC
    }

    public enum ReserveDayStatus {
        AVAILABLE, ACTIVATED, USED, RELEASED, EXPIRED
    }
}
