package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sports_karate_class")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateClass extends BaseAuditEntity {

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
    @JoinColumn(name = "conducted_by_user_id")
    private AppUser conductedBy;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassStatus status = ClassStatus.SCHEDULED;

    @Column(length = 200)
    private String topic;

    @Column(name = "class_notes", length = 1000)
    private String classNotes;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    public enum ClassStatus { SCHEDULED, ONGOING, COMPLETED, CANCELLED }
}
