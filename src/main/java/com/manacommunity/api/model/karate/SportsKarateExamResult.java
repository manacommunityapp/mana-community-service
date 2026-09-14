package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_karate_exam_result",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_exam_result_exam_enrollment",
                columnNames = {"exam_id", "enrollment_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateExamResult extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private SportsKarateGradingExam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private SportsKarateEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_belt_id")
    private SportsKarateBelt newBelt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private AppUser gradedBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 500)
    private String remarks;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    /** Guards against double belt promotion if results are resubmitted. */
    @Column(name = "belt_updated", nullable = false)
    @Builder.Default
    private Boolean beltUpdated = false;
}
