package com.manacommunity.api.notification.entity;

import com.manacommunity.api.notification.enums.CampaignStatus;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_sms_campaign", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSmsCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private SmsLanguage language = SmsLanguage.EN;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    @Builder.Default
    private MessageType messageType = MessageType.PROMOTIONAL;

    /** JSON array of recipient filters, e.g. {"communityId": 1, "role": "MEMBER"} */
    @Column(name = "recipient_filter", columnDefinition = "TEXT")
    private String recipientFilter;

    /** Total recipients resolved at scheduling time */
    @Column(name = "total_recipients")
    @Builder.Default
    private int totalRecipients = 0;

    @Column(name = "sent_count")
    @Builder.Default
    private int sentCount = 0;

    @Column(name = "delivered_count")
    @Builder.Default
    private int deliveredCount = 0;

    @Column(name = "failed_count")
    @Builder.Default
    private int failedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "estimated_cost_usd", precision = 12, scale = 4)
    private BigDecimal estimatedCostUsd;

    @Column(name = "actual_cost_usd", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal actualCostUsd = BigDecimal.ZERO;

    /** Template variable values as JSON map */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
