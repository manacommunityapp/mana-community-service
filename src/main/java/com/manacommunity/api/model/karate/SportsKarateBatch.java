package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsCourt;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "sports_karate_batch")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateBatch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private SportsKarateProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private SportsCourt court;

    @Column(name = "batch_name", nullable = false, length = 80)
    private String batchName;

    /** Comma-separated day abbreviations: MON,WED,FRI */
    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BatchStatus status = BatchStatus.UPCOMING;

    @Column(name = "attendance_threshold", nullable = false)
    @Builder.Default
    private Integer attendanceThreshold = 75;

    @Column(name = "auto_generate_classes", nullable = false)
    @Builder.Default
    private Boolean autoGenerateClasses = true;

    public enum BatchStatus { UPCOMING, ACTIVE, COMPLETED, CANCELLED }
}
