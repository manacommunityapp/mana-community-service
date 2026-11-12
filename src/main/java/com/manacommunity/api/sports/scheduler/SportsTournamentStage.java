package com.manacommunity.api.sports.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_tournament_stage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsTournamentStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private SportsTournamentConfig config;

    @Column(nullable = false)
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageType stageType;

    @Column(nullable = false)
    private Integer stageOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StageStatus stageStatus = StageStatus.NOT_GENERATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SchedulingMode schedulingMode = SchedulingMode.HYBRID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PublicationStatus publicationStatus = PublicationStatus.UNPUBLISHED;

    private Long publishedVersionId;

    private Long qualificationRuleId;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ─── Enums ───────────────────────────────────────────────────────────────

    public enum StageType {
        GROUP_STAGE,
        ROUND_OF_64, ROUND_OF_32, ROUND_OF_16,
        QUARTER_FINAL, SEMI_FINAL, THIRD_PLACE, FINAL,
        LEAGUE_MATCH, SWISS_ROUND,
        ELIMINATOR, QUALIFIER, SUPER_FINAL
    }

    public enum StageStatus {
        NOT_GENERATED, DRAFT, VALIDATED, APPROVED, PUBLISHED, COMPLETED, CANCELLED
    }

    public enum SchedulingMode {
        FULL_UPFRONT, STAGE_WISE, HYBRID
    }

    public enum PublicationStatus {
        UNPUBLISHED, PUBLISHED, UPDATED, WITHDRAWN
    }
}
