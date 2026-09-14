package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sports_karate_grading_exam")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateGradingExam extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private SportsKarateBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_belt_id", nullable = false)
    private SportsKarateBelt targetBelt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "examiner_name", length = 100)
    private String examinerName;

    @Column(name = "max_candidates")
    private Integer maxCandidates;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ExamStatus status = ExamStatus.SCHEDULED;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(length = 1000)
    private String notes;

    public enum ExamStatus { SCHEDULED, COMPLETED, CANCELLED }
}
