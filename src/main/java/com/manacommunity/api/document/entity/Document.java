package com.manacommunity.api.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "document")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "document_category")
    private DocumentCategory category;

    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "document_status")
    private DocumentStatus status = DocumentStatus.PENDING_SCAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, columnDefinition = "doc_access_level")
    private DocAccessLevel accessLevel = DocAccessLevel.OWNER_ONLY;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "reminder_days_before", nullable = false)
    private int reminderDaysBefore = 30;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Column(name = "legal_hold", nullable = false)
    private boolean legalHold = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
