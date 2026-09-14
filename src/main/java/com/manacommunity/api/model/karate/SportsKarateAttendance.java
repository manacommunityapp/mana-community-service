package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_karate_attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_attendance_class_enrollment",
                columnNames = {"class_id", "enrollment_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateAttendance extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportsKarateClass classSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private SportsKarateEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private AppUser markedBy;

    @Column(length = 300)
    private String notes;

    public enum AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }
}
