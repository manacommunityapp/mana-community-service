package com.manacommunity.api.sports.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_external_response",
        uniqueConstraints = @UniqueConstraint(name = "uq_sef_resp", columnNames = {"form_id", "external_response_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsExternalResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private SportsExternalForm form;

    @Column(name = "external_response_id", nullable = false, length = 255)
    private String externalResponseId;

    @Column(name = "respondent_email", length = 255)
    private String respondentEmail;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "raw_payload_json", nullable = false, columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ResponseStatus status = ResponseStatus.RECEIVED;

    @Column(name = "review_reason", length = 255)
    private String reviewReason;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_registration_id")
    private SportsEventRegistration linkedRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_user_id")
    private AppUser resolvedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ResponseStatus { RECEIVED, PROCESSING, IMPORTED, NEEDS_REVIEW, REJECTED }
}
