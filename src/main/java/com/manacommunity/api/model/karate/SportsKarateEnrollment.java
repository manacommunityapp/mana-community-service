package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_karate_enrollment",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_enrollment_batch_student",
                columnNames = {"batch_id", "student_user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateEnrollment extends BaseAuditEntity {

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
    @JoinColumn(name = "student_user_id", nullable = false)
    private AppUser student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_belt_id")
    private SportsKarateBelt currentBelt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by_user_id")
    private AppUser enrolledBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "enrolled_at", nullable = false)
    @Builder.Default
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "attendance_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal attendancePercentage = BigDecimal.ZERO;

    @Column(name = "total_classes_attended", nullable = false)
    @Builder.Default
    private Integer totalClassesAttended = 0;

    @Column(name = "grading_eligible", nullable = false)
    @Builder.Default
    private Boolean gradingEligible = false;

    @Column(name = "last_low_att_alert_at")
    private LocalDateTime lastLowAttAlertAt;

    @Column(length = 500)
    private String notes;

    public enum EnrollmentStatus { ACTIVE, WITHDRAWN, COMPLETED, SUSPENDED }
}
