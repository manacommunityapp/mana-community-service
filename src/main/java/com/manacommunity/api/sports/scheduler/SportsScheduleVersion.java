package com.manacommunity.api.sports.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_schedule_version")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsScheduleVersion {

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
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String changeReason;

    @Column(columnDefinition = "TEXT")
    private String changedMatchIdsJson;

    @Column(columnDefinition = "TEXT")
    private String changedFieldsJson;

    private String generatedBy;
    private String approvedBy;
    private String publishedBy;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum VersionStatus {
        DRAFT, VALIDATED, APPROVED, PUBLISHED, SUPERSEDED
    }
}
