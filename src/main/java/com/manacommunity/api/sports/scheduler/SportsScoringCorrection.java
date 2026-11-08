package com.manacommunity.api.sports.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_scoring_correction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsScoringCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @Column(name = "original_event_id")
    private Long originalEventId;

    @Column(name = "correction_type", nullable = false, length = 50)
    private String correctionType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "corrected_by")
    private Long correctedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
