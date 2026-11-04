package com.manacommunity.api.sports.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sports_scoring_command")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsScoringCommand {

    @Id
    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @Builder.Default
    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo = 1L;

    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "response_snapshot", columnDefinition = "TEXT")
    private String responseSnapshot;

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    void onCreate() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
    }
}
