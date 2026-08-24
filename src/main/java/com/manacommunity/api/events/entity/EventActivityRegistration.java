package com.manacommunity.api.events.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_activity_registrations", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventActivityRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private EventProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "head_count")
    @Builder.Default
    private Integer headCount = 1;

    @Column(name = "registration_type", length = 30)
    @Builder.Default
    private String registrationType = "individual";

    @Column(name = "primary_name", length = 200)
    private String primaryName;

    @Column(name = "primary_email", length = 200)
    private String primaryEmail;

    @Column(name = "primary_phone", length = 50)
    private String primaryPhone;

    @Column(name = "gotram", length = 100)
    private String gotram;

    @Column(name = "participant_data", columnDefinition = "TEXT")
    private String participantData;

    @Column(name = "custom_data", columnDefinition = "TEXT")
    private String customData;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActivityRegStatus status = ActivityRegStatus.CONFIRMED;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }

    public enum ActivityRegStatus { PENDING, CONFIRMED, WAITLISTED, DECLINED, CANCELLED }
}
