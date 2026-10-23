package com.manacommunity.api.events.entity;

import com.manacommunity.api.events.enums.RegistrationSource;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_cultural_registrations")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCulturalRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_code", unique = true, nullable = false, length = 100)
    private String regCode;

    @Column(name = "cultural_event_id", nullable = false)
    private Long culturalEventId;

    @Column(name = "main_event_id")
    private Long mainEventId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "participant_name", nullable = false, length = 255)
    private String participantName;

    @Column(name = "gotram", length = 100)
    private String gotram;

    @Column(name = "devotee_count")
    @Builder.Default
    private Integer devoteeCount = 1;

    @Column(name = "members_json", columnDefinition = "TEXT")
    private String membersJson;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "CONFIRMED";

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false, length = 20)
    @Builder.Default
    private RegistrationSource registrationSource = RegistrationSource.SELF;

    @Column(name = "registered_by")
    private Long registeredBy;

    @Column(name = "override_used", nullable = false)
    @Builder.Default
    private Boolean overrideUsed = false;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "checked_in")
    @Builder.Default
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (overrideUsed == null) overrideUsed = false;
        if (registrationSource == null) registrationSource = RegistrationSource.SELF;
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
