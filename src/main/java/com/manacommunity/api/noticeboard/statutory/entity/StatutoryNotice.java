package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statutory_notices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatutoryNotice extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    private NoticePriority priority;

    @Enumerated(EnumType.STRING)
    private NoticeStatus status;

    private boolean statutory;
    private boolean readReceiptRequired;

    private String publishedBy;
    private String publishedByRole;
    private LocalDateTime publishedAt;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;

    private String attachmentUrl;
    private String attachmentName;

    private Long audienceGroupId;
    private int totalRecipients;
    private int deliveredCount;
    private int openedCount;

    private boolean pinned;
    private int displayOrder;

    public enum NoticeCategory {
        AGM_SUMMONS, MAINTENANCE_ALERT, WATER_SUPPLY, PARKING_RULES,
        FIRE_SAFETY, PEST_CONTROL, SOCIETY_ELECTION, CONSTRUCTION_WORK,
        FESTIVAL_EVENT, GENERAL_CIRCULAR, LEGAL_COMPLIANCE, EMERGENCY
    }

    public enum NoticePriority { CRITICAL, HIGH, NORMAL, LOW }

    public enum NoticeStatus { DRAFT, SCHEDULED, PUBLISHED, EXPIRED, RECALLED }
}
